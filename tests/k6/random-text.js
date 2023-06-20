import http from 'k6/http';
import { check } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.1.0/index.js';

const params = {
  headers: {
    'Content-Type': 'application/json'
  }
}

export default function(data) {
  var req = { 'text': randomString(50, 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ') }
  var res = http.post('http://localhost:8888/analyze/',
      JSON.stringify(req),
      params);
  check(res, {
      'status is 200': (r) => r.status === 200
  });
}

