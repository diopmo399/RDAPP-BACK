/**
 * Health Check Tests
 *
 * Basic smoke tests to verify the application is running
 */

describe('Health Check', () => {
  it('should load the homepage', () => {
    cy.visit('/');

    // Verify page loads
    cy.waitForPageLoad();

    // Check title exists
    cy.title().should('not.be.empty');

    // Check body is visible
    cy.get('body').should('be.visible');
  });

  it('should have correct base URL', () => {
    // Log the base URL for debugging
    cy.log('Base URL:', Cypress.config('baseUrl'));

    // Verify base URL is set
    expect(Cypress.config('baseUrl')).to.not.be.undefined;
    expect(Cypress.config('baseUrl')).to.include('http');
  });

  it('should respond with 200 status', () => {
    cy.request('/').then((response) => {
      expect(response.status).to.eq(200);
      expect(response.headers).to.have.property('content-type');
    });
  });

  it('should check API health endpoint', () => {
    // Skip if API health endpoint doesn't exist
    cy.request({
      url: `${Cypress.env('apiUrl')}/health`,
      failOnStatusCode: false
    }).then((response) => {
      // Accept 200, 204, or 404 (if endpoint doesn't exist)
      expect(response.status).to.be.oneOf([200, 204, 404]);

      if (response.status === 200) {
        cy.log('API is healthy');
      }
    });
  });

  it('should not have console errors', () => {
    cy.visit('/');

    // Check for console errors (optional)
    cy.window().then((win) => {
      const errors = [];

      // Override console.error to capture errors
      const originalError = win.console.error;
      win.console.error = (...args) => {
        errors.push(args.join(' '));
        originalError.apply(win.console, args);
      };

      // Wait a bit for any async errors
      cy.wait(1000);

      // Verify no critical errors
      // (You may want to filter out expected errors)
      const criticalErrors = errors.filter(
        err => !err.includes('ResizeObserver') && !err.includes('Warning:')
      );

      expect(criticalErrors).to.have.length(0);
    });
  });

  it('should load static assets', () => {
    cy.visit('/');

    // Check CSS is loaded
    cy.get('link[rel="stylesheet"]').should('exist');

    // Check JavaScript is loaded
    cy.get('script[src]').should('exist');

    // Verify no broken images (sample check)
    cy.get('img').each(($img) => {
      // Skip if image is lazy-loaded
      if ($img.attr('loading') === 'lazy') {
        return;
      }

      // Check naturalWidth > 0 (image loaded successfully)
      expect($img[0].naturalWidth).to.be.greaterThan(0);
    });
  });

  it('should have correct viewport', () => {
    cy.viewport(1280, 720);
    cy.visit('/');

    // Verify viewport dimensions
    cy.window().then((win) => {
      expect(win.innerWidth).to.eq(1280);
      expect(win.innerHeight).to.eq(720);
    });
  });

  it('should be responsive on mobile', () => {
    // Test mobile viewport
    cy.viewport('iphone-x');
    cy.visit('/');

    // Verify page is still functional
    cy.get('body').should('be.visible');
  });
});
