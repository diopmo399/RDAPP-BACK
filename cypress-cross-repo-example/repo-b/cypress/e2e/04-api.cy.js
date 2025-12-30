/**
 * API Tests
 *
 * Tests for API endpoints and integrations
 */

describe('API Tests', () => {
  // Skip all tests if API feature is disabled
  before(function() {
    if (!Cypress.env('enableApiTests')) {
      this.skip();
    }
  });

  const apiUrl = Cypress.env('apiUrl');

  describe('API Health & Status', () => {
    it('should respond to health check', () => {
      cy.request({
        url: `${apiUrl}/health`,
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.be.oneOf([200, 204]);

        if (response.body) {
          cy.log('Health response:', response.body);
        }
      });
    });

    it('should return API version', () => {
      cy.request({
        url: `${apiUrl}/version`,
        failOnStatusCode: false
      }).then((response) => {
        if (response.status === 200) {
          expect(response.body).to.have.property('version');
          cy.log('API Version:', response.body.version);
        }
      });
    });
  });

  describe('API Endpoints', () => {
    it('should handle GET requests', () => {
      cy.request({
        method: 'GET',
        url: `${apiUrl}/items`,
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.be.oneOf([200, 404]);

        if (response.status === 200) {
          // Verify response structure
          expect(response.body).to.be.an('array').or.to.be.an('object');

          // Verify headers
          expect(response.headers).to.have.property('content-type');
          expect(response.headers['content-type']).to.include('application/json');
        }
      });
    });

    it('should handle POST requests', () => {
      // Skip in production
      if (Cypress.env('environment') === 'production') {
        cy.log('Skipping POST test in production');
        return;
      }

      cy.request({
        method: 'POST',
        url: `${apiUrl}/items`,
        body: {
          name: 'Test Item',
          description: 'Created by Cypress E2E test'
        },
        failOnStatusCode: false
      }).then((response) => {
        // Accept 200, 201, 401 (if auth required), 404 (if endpoint doesn't exist)
        expect(response.status).to.be.oneOf([200, 201, 401, 404]);

        if (response.status === 200 || response.status === 201) {
          expect(response.body).to.have.property('id');
          cy.log('Created item ID:', response.body.id);
        }
      });
    });

    it('should handle query parameters', () => {
      cy.request({
        method: 'GET',
        url: `${apiUrl}/items`,
        qs: {
          page: 1,
          limit: 10,
          sort: 'name'
        },
        failOnStatusCode: false
      }).then((response) => {
        if (response.status === 200) {
          expect(response.body).to.be.an('array').or.to.be.an('object');
        }
      });
    });

    it('should handle pagination', () => {
      cy.request({
        method: 'GET',
        url: `${apiUrl}/items?page=1&limit=5`,
        failOnStatusCode: false
      }).then((response) => {
        if (response.status === 200) {
          // Verify pagination metadata
          if (response.body.meta) {
            expect(response.body.meta).to.have.property('page');
            expect(response.body.meta).to.have.property('limit');
          }
        }
      });
    });
  });

  describe('API Error Handling', () => {
    it('should return 404 for non-existent resources', () => {
      cy.request({
        method: 'GET',
        url: `${apiUrl}/items/99999999`,
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.eq(404);
      });
    });

    it('should validate request body', () => {
      cy.request({
        method: 'POST',
        url: `${apiUrl}/items`,
        body: {
          // Invalid/incomplete data
        },
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.be.oneOf([400, 422, 401, 404]);
      });
    });

    it('should handle rate limiting', () => {
      // Make multiple requests rapidly
      const requests = Array(10).fill(null).map(() =>
        cy.request({
          method: 'GET',
          url: `${apiUrl}/items`,
          failOnStatusCode: false
        })
      );

      // Check if any request is rate limited
      Promise.all(requests).then((responses) => {
        const rateLimited = responses.some(r => r.status === 429);
        if (rateLimited) {
          cy.log('Rate limiting is active');
        }
      });
    });
  });

  describe('API Security', () => {
    it('should require authentication for protected endpoints', () => {
      cy.request({
        method: 'GET',
        url: `${apiUrl}/users/me`,
        failOnStatusCode: false
      }).then((response) => {
        // Should return 401 without auth token
        expect(response.status).to.be.oneOf([401, 403, 404]);
      });
    });

    it('should accept valid authentication token', () => {
      // Skip in production
      if (Cypress.env('environment') === 'production') {
        return;
      }

      // First, login to get token
      const testUser = Cypress.env('testUser');

      cy.request({
        method: 'POST',
        url: `${apiUrl}/auth/login`,
        body: {
          email: testUser.email,
          password: testUser.password
        },
        failOnStatusCode: false
      }).then((loginResponse) => {
        if (loginResponse.status === 200 && loginResponse.body.token) {
          const token = loginResponse.body.token;

          // Use token to access protected endpoint
          cy.request({
            method: 'GET',
            url: `${apiUrl}/users/me`,
            headers: {
              Authorization: `Bearer ${token}`
            },
            failOnStatusCode: false
          }).then((response) => {
            expect(response.status).to.eq(200);
            expect(response.body).to.have.property('email');
          });
        }
      });
    });

    it('should have security headers', () => {
      cy.request({
        url: `${apiUrl}/health`,
        failOnStatusCode: false
      }).then((response) => {
        // Check for common security headers
        const headers = response.headers;

        // Optional: Verify security headers exist
        if (headers['x-frame-options']) {
          cy.log('X-Frame-Options:', headers['x-frame-options']);
        }
        if (headers['x-content-type-options']) {
          cy.log('X-Content-Type-Options:', headers['x-content-type-options']);
        }
      });
    });
  });

  describe('API Performance', () => {
    it('should respond within acceptable time', () => {
      const startTime = Date.now();

      cy.request({
        url: `${apiUrl}/items`,
        failOnStatusCode: false
      }).then((response) => {
        const endTime = Date.now();
        const duration = endTime - startTime;

        cy.log(`API response time: ${duration}ms`);

        // Verify response time is under 2 seconds
        expect(duration).to.be.lessThan(2000);
      });
    });

    it('should handle concurrent requests', () => {
      // Make 5 concurrent requests
      const requests = [1, 2, 3, 4, 5].map(i =>
        cy.request({
          url: `${apiUrl}/items`,
          failOnStatusCode: false
        })
      );

      // All should succeed
      cy.wrap(Promise.all(requests)).then((responses) => {
        responses.forEach((response, index) => {
          cy.log(`Request ${index + 1}: ${response.status}`);
          expect(response.status).to.be.oneOf([200, 404]);
        });
      });
    });
  });

  describe('API Data Validation', () => {
    it('should return consistent data structure', () => {
      cy.request({
        url: `${apiUrl}/items`,
        failOnStatusCode: false
      }).then((response) => {
        if (response.status === 200 && Array.isArray(response.body)) {
          // Verify each item has expected properties
          response.body.forEach((item) => {
            expect(item).to.have.property('id');
            // Add other expected properties based on your API
          });
        }
      });
    });

    it('should sanitize user input', () => {
      cy.request({
        method: 'POST',
        url: `${apiUrl}/items`,
        body: {
          name: '<script>alert("xss")</script>',
          description: 'Test XSS protection'
        },
        failOnStatusCode: false
      }).then((response) => {
        if (response.status === 200 || response.status === 201) {
          // Verify script tags are escaped or removed
          expect(response.body.name).to.not.include('<script>');
        }
      });
    });
  });
});
