/**
 * Authentication Tests
 *
 * Tests for login, logout, and authentication flows
 */

describe('Authentication', () => {
  // Skip all tests if login feature is disabled
  before(function() {
    if (!Cypress.env('enableLoginTests')) {
      this.skip();
    }
  });

  beforeEach(() => {
    cy.visit('/');
    cy.clearCookies();
    cy.clearLocalStorage();
  });

  describe('Login Page', () => {
    it('should display login form', () => {
      cy.visit('/login', { failOnStatusCode: false });

      // Check form elements exist
      cy.get('[data-cy=email], input[type="email"], input[name="email"]', {
        timeout: 5000
      }).should('exist');

      cy.get('[data-cy=password], input[type="password"], input[name="password"]')
        .should('exist');

      cy.get('[data-cy=submit], button[type="submit"]')
        .should('exist');
    });

    it('should show validation errors for empty form', () => {
      cy.visit('/login', { failOnStatusCode: false });

      // Submit empty form
      cy.get('button[type="submit"]').click();

      // Check for error messages
      cy.contains(/required|obligatoire|email|mot de passe/i, { timeout: 3000 })
        .should('exist');
    });

    it('should show error for invalid credentials', () => {
      cy.visit('/login', { failOnStatusCode: false });

      // Enter invalid credentials
      cy.get('input[type="email"]').type('invalid@example.com');
      cy.get('input[type="password"]').type('wrongpassword');
      cy.get('button[type="submit"]').click();

      // Check for error message
      cy.contains(/invalid|incorrect|erreur|invalide/i, { timeout: 5000 })
        .should('exist');
    });

    it('should login with valid credentials', () => {
      // Skip in production
      if (Cypress.env('environment') === 'production') {
        cy.log('Skipping test in production');
        return;
      }

      cy.visit('/login', { failOnStatusCode: false });

      // Use test user from config
      const testUser = Cypress.env('testUser');

      cy.get('input[type="email"]').type(testUser.email);
      cy.get('input[type="password"]').type(testUser.password);
      cy.get('button[type="submit"]').click();

      // Verify redirect after login
      cy.url({ timeout: 10000 }).should('not.include', '/login');

      // Verify user is logged in
      cy.getCookie('session').should('exist');
    });

    it('should toggle password visibility', () => {
      cy.visit('/login', { failOnStatusCode: false });

      const passwordInput = cy.get('input[type="password"]');
      passwordInput.type('SecretPassword123');

      // Click toggle button (adjust selector)
      cy.get('[data-cy=toggle-password], button[aria-label*="password"]', {
        timeout: 3000
      }).click({ force: true });

      // Verify input type changed
      cy.get('input[name="password"]').should('have.attr', 'type', 'text');
    });
  });

  describe('Session Management', () => {
    it('should persist session across page reloads', () => {
      // Skip in production
      if (Cypress.env('environment') === 'production') {
        return;
      }

      // Login using custom command
      const testUser = Cypress.env('testUser');
      cy.login(testUser.email, testUser.password);

      cy.visit('/dashboard', { failOnStatusCode: false });

      // Reload page
      cy.reload();

      // Verify still logged in
      cy.url().should('not.include', '/login');
    });

    it('should redirect to login when accessing protected page', () => {
      // Visit protected page without login
      cy.visit('/dashboard', { failOnStatusCode: false });

      // Should redirect to login
      cy.url({ timeout: 5000 }).should('include', '/login');
    });

    it('should logout successfully', () => {
      // Skip in production
      if (Cypress.env('environment') === 'production') {
        return;
      }

      // Login first
      const testUser = Cypress.env('testUser');
      cy.login(testUser.email, testUser.password);

      // Click logout button
      cy.get('[data-cy=logout], button:contains("Logout"), a:contains("Déconnexion")', {
        timeout: 5000
      }).click({ force: true });

      // Verify logged out
      cy.url({ timeout: 5000 }).should('include', '/login');
      cy.getCookie('session').should('not.exist');
    });
  });

  describe('Password Reset', () => {
    it('should display password reset form', () => {
      cy.visit('/forgot-password', { failOnStatusCode: false });

      cy.get('input[type="email"]', { timeout: 3000 }).should('exist');
      cy.get('button[type="submit"]').should('exist');
    });

    it('should validate email format', () => {
      cy.visit('/forgot-password', { failOnStatusCode: false });

      cy.get('input[type="email"]').type('invalid-email');
      cy.get('button[type="submit"]').click();

      cy.contains(/invalid|invalide|format/i, { timeout: 3000 }).should('exist');
    });
  });

  describe('API Authentication', () => {
    it('should authenticate via API', () => {
      // Skip in production
      if (Cypress.env('environment') === 'production') {
        return;
      }

      const testUser = Cypress.env('testUser');

      cy.request({
        method: 'POST',
        url: `${Cypress.env('apiUrl')}/auth/login`,
        body: {
          email: testUser.email,
          password: testUser.password
        },
        failOnStatusCode: false
      }).then((response) => {
        // Verify response
        expect(response.status).to.be.oneOf([200, 201]);

        // Check for token or session
        expect(response.body).to.have.property('token')
          .or.to.have.property('session')
          .or.to.have.property('user');
      });
    });

    it('should reject invalid API credentials', () => {
      cy.request({
        method: 'POST',
        url: `${Cypress.env('apiUrl')}/auth/login`,
        body: {
          email: 'invalid@example.com',
          password: 'wrongpassword'
        },
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.be.oneOf([401, 403]);
      });
    });
  });
});
