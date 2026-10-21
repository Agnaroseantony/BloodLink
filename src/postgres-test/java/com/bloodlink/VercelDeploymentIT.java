package com.bloodlink;

import com.bloodlink.controller.form.RequestForm;
import com.bloodlink.model.*;
import com.bloodlink.service.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

/** Opt-in tests against real, temporary PostgreSQL, without Docker or cloud credentials. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VercelDeploymentIT {
    EmbeddedPostgres postgres;
    ServletWebServerApplicationContext first, second;
    final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @BeforeAll void start() throws IOException {
        postgres = EmbeddedPostgres.builder().setServerConfig("listen_addresses", "127.0.0.1").start();
        first = application(); second = application();
    }
    ServletWebServerApplicationContext application() {
        return (ServletWebServerApplicationContext) new SpringApplicationBuilder(BloodLinkApplication.class)
            .profiles("vercel").run("--server.port=0", "--spring.main.banner-mode=off",
                "--spring.datasource.url=" + postgres.getJdbcUrl("postgres", "postgres"),
                "--spring.datasource.username=postgres", "--spring.datasource.password=postgres",
                "--app.seed-demo=true");
    }
    @AfterAll void stop() throws IOException {
        if (first != null) first.close(); if (second != null) second.close();
        if (postgres != null) postgres.close();
    }
    HttpResponse<String> request(ServletWebServerApplicationContext app, String path, String cookie, Map<String,String> form) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.getWebServer().getPort() + path))
            .header("X-Forwarded-Proto", "https");
        if (cookie != null) request.header("Cookie", cookie);
        if (form != null) {
            String body = form.entrySet().stream().map(e -> encode(e.getKey()) + "=" + encode(e.getValue())).collect(java.util.stream.Collectors.joining("&"));
            request.header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body));
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    String csrf(String html) {
        Matcher matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertThat(matcher.find()).as("CSRF token in real rendered HTML").isTrue(); return matcher.group(1);
    }
    String cookie(HttpResponse<String> response) {
        return response.headers().allValues("set-cookie").stream().filter(c -> c.startsWith("SESSION="))
            .map(c -> c.substring(0,c.indexOf(';'))).findFirst().orElseThrow();
    }
    String login(String username) throws Exception {
        HttpResponse<String> page = request(first,"/login",null,null);
        HttpResponse<String> login = request(first,"/login",cookie(page),Map.of("username",username,"password","Demo@12345","_csrf",csrf(page.body())));
        assertThat(login.statusCode()).isEqualTo(302);
        assertThat(login.headers().firstValue("location").orElse("")).endsWith("/dashboard");
        assertThat(login.headers().allValues("set-cookie")).anyMatch(c -> c.contains("Secure") && c.contains("HttpOnly"));
        return cookie(login);
    }

    @Test @Order(1) void migrationSharedLoginCsrfWorkflowAndRestartWorkOnPostgres() throws Exception {
        JdbcTemplate jdbc = first.getBean(JdbcTemplate.class);
        assertThat(jdbc.queryForObject("select count(*) from app_users",Long.class)).isEqualTo(8);
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success=true",Long.class)).isEqualTo(1);
        String hospital = login("hospital.care"), donor = login("donor.rahul");
        assertThat(request(second,"/dashboard",hospital,null).statusCode()).isEqualTo(200);
        HttpResponse<String> form = request(second,"/hospital/requests/new",hospital,null);
        String deadline = BloodLinkService.now().plusDays(1).withNano(0).toString();
        HttpResponse<String> published = request(second,"/hospital/requests",hospital,Map.of("_csrf",csrf(form.body()),"bloodGroup","O_POS","unitsRequested","1","urgency","URGENT","requiredBy",deadline,"description","Fictional cloud deployment acceptance test."));
        assertThat(published.statusCode()).isEqualTo(302);
        String path = URI.create(published.headers().firstValue("location").orElseThrow()).getPath();
        Long id = Long.valueOf(path.substring(path.lastIndexOf('/')+1));
        HttpResponse<String> detail = request(first,path,donor,null);
        assertThat(request(first,"/donor/requests/"+id+"/respond",donor,Map.of("_csrf",csrf(detail.body()))).statusCode()).isEqualTo(302);
        assertThat(second.getBean(BloodLinkService.class).points("donor.rahul")).isZero();
        HttpResponse<String> manage = request(second,path,hospital,null);
        Matcher action = Pattern.compile("action=\"(/hospital/requests/\\d+/confirm/\\d+)\"").matcher(manage.body());
        assertThat(action.find()).isTrue();
        assertThat(request(first,action.group(1),hospital,Map.of("_csrf",csrf(manage.body()),"units","1")).statusCode()).isEqualTo(302);
        assertThat(second.getBean(BloodLinkService.class).request(id).getStatus()).isEqualTo(RequestStatus.FULFILLED);
        assertThat(second.getBean(BloodLinkService.class).points("donor.rahul")).isEqualTo(100);
        // A completely fresh application instance must recognize the same login and saved reward.
        first.close(); first = application();
        HttpResponse<String> rewards = request(first,"/donor/rewards",donor,null);
        assertThat(rewards.statusCode()).isEqualTo(200);
        assertThat(rewards.body()).contains("<span>100</span><small>points</small>","First Donation");
        assertThat(first.getBean(JdbcTemplate.class).queryForObject("select count(*) from app_users",Long.class)).isEqualTo(8);
    }

    @Test @Order(2) void differentInstancesCannotConfirmBeyondTarget() throws Exception {
        BloodLinkService a = first.getBean(BloodLinkService.class), b = second.getBean(BloodLinkService.class);
        RequestForm form = new RequestForm(); form.setBloodGroup(BloodGroup.O_POS); form.setUnitsRequested(1);
        form.setUrgency(Urgency.NORMAL); form.setRequiredBy(BloodLinkService.now().plusDays(1));
        BloodRequest request = a.publish("hospital.care",form);
        DonorResponse one = a.respond("donor.asha",request.getId()), two = b.respond("donor.rahul",request.getId());
        long before = a.points("donor.asha") + a.points("donor.rahul");
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<Boolean> firstResult = pool.submit(() -> confirm(a,request.getId(),one.getId(),start));
            Future<Boolean> secondResult = pool.submit(() -> confirm(b,request.getId(),two.getId(),start));
            start.countDown();
            assertThat(List.of(firstResult.get(20,TimeUnit.SECONDS),secondResult.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        }
        assertThat(a.request(request.getId()).getConfirmedUnits()).isEqualTo(1);
        assertThat(a.points("donor.asha") + a.points("donor.rahul")).isEqualTo(before+100);
    }
    boolean confirm(BloodLinkService service, Long request, Long response, CountDownLatch start) throws Exception {
        start.await();
        try { service.confirm("hospital.care",request,response,1); return true; }
        catch (BusinessException expected) { return false; }
    }
}
