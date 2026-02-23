import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { map } from 'rxjs/operators';

/**
 * Intercepteur qui normalise la casse entre le backend Java (UPPER_CASE enums)
 * et le frontend TypeScript (lower_case union types).
 *
 * Response : PLANNED → planned, IN_PROGRESS → in_progress, DEV → dev, DEVOPS → devops
 * Request  : planned → PLANNED, in_progress → IN_PROGRESS, dev → DEV, devops → DEVOPS
 */

const ENUM_FIELDS = new Set(['status', 'role']);

function toLower(value: string): string {
  return value.toLowerCase();
}

function toUpper(value: string): string {
  return value.toUpperCase();
}

function transformBody(body: any, transformFn: (v: string) => string): any {
  if (body == null) return body;

  if (Array.isArray(body)) {
    return body.map(item => transformBody(item, transformFn));
  }

  if (typeof body === 'object') {
    const result: any = {};
    for (const [key, value] of Object.entries(body)) {
      if (ENUM_FIELDS.has(key) && typeof value === 'string') {
        result[key] = transformFn(value);
      } else if (key === 'members' && Array.isArray(value)) {
        result[key] = value.map((m: any) => transformBody(m, transformFn));
      } else {
        result[key] = value;
      }
    }
    return result;
  }

  return body;
}

export const caseNormalizerInterceptor: HttpInterceptorFn = (req, next) => {
  // Ne traiter que les appels vers notre API config
  if (!req.url.includes('/api/v1/')) {
    return next(req);
  }

  // Outgoing: lower → UPPER pour les enums dans le body
  let outReq = req;
  if (req.body && typeof req.body === 'object') {
    outReq = req.clone({ body: transformBody(req.body, toUpper) });
  }

  return next(outReq).pipe(
    map(event => {
      // Incoming: UPPER → lower pour les enums dans la réponse
      if (event instanceof HttpResponse && event.body) {
        return event.clone({ body: transformBody(event.body, toLower) });
      }
      return event;
    }),
  );
};
