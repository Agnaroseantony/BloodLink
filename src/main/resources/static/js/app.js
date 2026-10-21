/* Progressive enhancement: forms and navigation work without JavaScript. */
document.addEventListener('DOMContentLoaded', () => {
  const menu = document.querySelector('.menu-toggle');
  menu?.addEventListener('click', () => {
    const open = document.querySelector('#sidebar').classList.toggle('is-open');
    menu.setAttribute('aria-expanded', String(open));
  });
  document.addEventListener('click', event => {
    if (menu && !event.target.closest('.sidebar') && !event.target.closest('.menu-toggle')) {
      document.querySelector('#sidebar').classList.remove('is-open');
      menu.setAttribute('aria-expanded', 'false');
    }
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && menu) {
      document.querySelector('#sidebar').classList.remove('is-open');
      menu.setAttribute('aria-expanded', 'false');
    }
  });
  document.querySelectorAll('form').forEach(form => form.addEventListener('submit', event => {
    if (form.dataset.confirm && !window.confirm(form.dataset.confirm)) { event.preventDefault(); return; }
    const submit = form.querySelector('button[type="submit"]');
    if (submit) { submit.disabled = true; submit.textContent = 'Working…'; }
  }));
  window.addEventListener('pageshow', () => {
    // A full reload restores submit labels after returning from browser history.
    if ([...document.querySelectorAll('button[type="submit"]')].some(button => button.disabled)) window.location.reload();
  });

  document.querySelectorAll('.map').forEach(element => {
    const status = element.parentElement.querySelector('.map-status');
    const picker = element.dataset.picker === 'true';
    const latInput = picker ? document.querySelector('#latitude') : null;
    const lngInput = picker ? document.querySelector('#longitude') : null;
    const lat = picker ? latInput?.value : element.dataset.lat;
    const lng = picker ? lngInput?.value : element.dataset.lng;
    const valid = (a, b) => a !== '' && b !== '' && a != null && b != null && Number.isFinite(Number(a)) && Number.isFinite(Number(b)) && Math.abs(Number(a)) <= 90 && Math.abs(Number(b)) <= 180;
    let map = null, marker = null;
    const setPoint = (a, b, move = false) => {
      if (!valid(a, b)) return;
      if (picker) { latInput.value = Number(a).toFixed(6); lngInput.value = Number(b).toFixed(6); }
      if (!map) return;
      if (marker) marker.setLatLng([Number(a), Number(b)]);
      else marker = L.circleMarker([Number(a), Number(b)], {radius:10, color:'#fff', weight:3, fillColor:'#c62845', fillOpacity:1}).addTo(map);
      if (move) map.setView([Number(a), Number(b)], 13);
    };
    if (typeof L !== 'undefined') {
      element.textContent = '';
      map = L.map(element, {scrollWheelZoom:false}).setView(valid(lat, lng) ? [Number(lat), Number(lng)] : [12.9716,77.5946], 12);
      const attributionLink = document.createElement('a');
      attributionLink.href = element.dataset.attributionUrl;
      attributionLink.textContent = element.dataset.attribution;
      attributionLink.target = '_blank'; attributionLink.rel = 'noopener';
      const tiles = L.tileLayer(element.dataset.tileUrl, {maxZoom:19, attribution:'© ' + attributionLink.outerHTML}).addTo(map);
      tiles.on('tileerror', () => { if (status) status.textContent = 'Map tiles could not load. Use the written address or enter coordinates manually. Online maps need internet access.'; });
      setPoint(lat, lng);
      if (picker) map.on('click', event => {
        setPoint(event.latlng.lat, event.latlng.lng);
        status.textContent = 'Location selected. Save the form to keep these coordinates.';
      });
    }
    if (!picker) return;
    [latInput, lngInput].forEach(input => input.addEventListener('change', () => setPoint(latInput.value, lngInput.value, true)));
    element.parentElement.querySelector('[data-clear-location]')?.addEventListener('click', () => {
      latInput.value = ''; lngInput.value = '';
      if (map && marker) { map.removeLayer(marker); marker = null; }
      status.textContent = 'Coordinates cleared. Save your profile to apply the change.';
    });
    element.parentElement.querySelector('[data-geolocate]')?.addEventListener('click', event => {
      if (!navigator.geolocation) { status.textContent = 'Location is unavailable. Enter your city and select a map location manually.'; return; }
      const button = event.currentTarget; button.disabled = true;
      status.textContent = 'Waiting for your location permission…';
      // Permission is requested only in response to this deliberate button click.
      navigator.geolocation.getCurrentPosition(position => {
        setPoint(position.coords.latitude, position.coords.longitude, true);
        status.textContent = 'Location selected. Your city stays as entered. Save the form to keep your changes.';
        button.disabled = false;
      }, () => {
        status.textContent = 'Location could not be shared. You can enter your city and coordinates or click the map instead.';
        button.disabled = false;
      }, {enableHighAccuracy:false,timeout:12000,maximumAge:60000});
    });
  });

  if (document.body.dataset.donor !== 'true') return;
  let previous = Number(document.querySelector('[data-unread]')?.textContent || 0);
  let polling = false;
  const poll = async () => {
    if (document.hidden || polling) return;
    polling = true;
    try {
      const response = await fetch('/api/notifications/count', {headers:{Accept:'application/json'}});
      if (!response.ok || response.redirected || !response.headers.get('content-type')?.includes('application/json')) throw new Error('Session or connection unavailable');
      const data = await response.json();
      document.querySelectorAll('[data-unread]').forEach(element => { element.textContent = data.unread; });
      const note = document.querySelector('[data-poll-status]');
      if (note) note.textContent = 'Up to date. Checking for new alerts every 20 seconds.';
      if (data.unread > previous) {
        const toast = document.querySelector('.live-toast');
        if (toast) {
          toast.textContent = 'A hospital published a new request matching your profile.';
          const link = document.createElement('a'); link.href = '/donor/notifications'; link.textContent = 'View your alerts ↗';
          toast.appendChild(link); toast.classList.add('visible');
          window.setTimeout(() => toast.classList.remove('visible'), 12000);
        }
      }
      previous = data.unread;
    } catch (_) {
      const note = document.querySelector('[data-poll-status]');
      if (note) note.textContent = 'Could not refresh alerts. Check your connection or sign in again. Retrying automatically.';
    } finally { polling = false; }
  };
  window.setInterval(poll, 20000);
  document.addEventListener('visibilitychange', () => { if (!document.hidden) poll(); });
});
