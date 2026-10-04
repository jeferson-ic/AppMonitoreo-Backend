// Prueba de carga con k6: https://k6.io
// Uso: k6 run -e BASE_URL=https://<servicio>.onrender.com -e CORREO=... -e CONTRASENA=... pruebas/carga.js
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 30 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<2000'],
    'http_req_duration{endpoint:crear}': ['p(95)<5000'],
  },
};

export function setup() {
  const res = http.post(`${BASE_URL}/auth/login`,
    JSON.stringify({ correo: __ENV.CORREO, contrasena: __ENV.CONTRASENA }),
    { headers: { 'Content-Type': 'application/json' } });
  check(res, { 'login 200': (r) => r.status === 200 });
  return { token: res.json('token') };
}

export default function (data) {
  const params = {
    headers: { Authorization: `Bearer ${data.token}`, 'Content-Type': 'application/json' },
  };
  const lat = -12.05 + Math.random() * 0.02;
  const lng = -77.05 + Math.random() * 0.02;

  const listar = http.get(`${BASE_URL}/incidentes`, { ...params, tags: { endpoint: 'listar' } });
  check(listar, { 'listar 200': (r) => r.status === 200 });

  const cercanos = http.get(`${BASE_URL}/incidentes/cercanos?lat=${lat}&lng=${lng}&radioKm=1`,
    { ...params, tags: { endpoint: 'cercanos' } });
  check(cercanos, { 'cercanos 200': (r) => r.status === 200 });

  const alerta = http.get(`${BASE_URL}/alertas/verificar?lat=${lat}&lng=${lng}`,
    { ...params, tags: { endpoint: 'alerta' } });
  check(alerta, { 'alerta 200': (r) => r.status === 200 });

  const crear = http.post(`${BASE_URL}/incidentes`,
    JSON.stringify({ tipoIncidente: 'Robo', descripcion: 'Prueba de carga', latitud: lat, longitud: lng }),
    { ...params, tags: { endpoint: 'crear' } });
  check(crear, { 'crear 201': (r) => r.status === 201 });

  sleep(1);
}
