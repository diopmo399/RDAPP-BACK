const { defineConfig } = require('cypress');

module.exports = defineConfig({
  e2e: {
    // Base URL sera override par le workflow GitHub Actions
    // Via: --config baseUrl=https://deployed-url.com
    baseUrl: 'http://localhost:3000',

    // Timeouts
    defaultCommandTimeout: 10000,
    pageLoadTimeout: 60000,
    requestTimeout: 10000,
    responseTimeout: 30000,

    // Viewport
    viewportWidth: 1280,
    viewportHeight: 720,

    // Video & Screenshots
    video: true,
    videoCompression: 32,
    videosFolder: 'cypress/videos',
    screenshotsFolder: 'cypress/screenshots',
    screenshotOnRunFailure: true,

    // Reporter
    reporter: 'cypress-multi-reporters',
    reporterOptions: {
      reporterEnabled: 'spec, json',
      jsonReporterOptions: {
        toConsole: false,
        output: 'cypress/results/test-results.json'
      }
    },

    // Retry on failure (CI only)
    retries: {
      runMode: 2,    // 2 retries en CI
      openMode: 0    // 0 retry en mode interactif
    },

    // Test isolation
    testIsolation: true,

    // Experimental
    experimentalMemoryManagement: true,
    numTestsKeptInMemory: 10,

    setupNodeEvents(on, config) {
      // Plugin configuration

      // Log base URL
      console.log('Cypress Base URL:', config.baseUrl);

      // Environment-specific configuration
      if (config.env.environment === 'staging') {
        config.defaultCommandTimeout = 15000;
      } else if (config.env.environment === 'production') {
        config.defaultCommandTimeout = 20000;
        config.retries.runMode = 3;
      }

      return config;
    },
  },

  env: {
    // Variables d'environnement custom
    // Peuvent être override via CYPRESS_xxx ou --env xxx=value

    // API endpoints
    apiUrl: '/api',

    // Test users (staging/preprod only)
    testUser: {
      email: 'test@example.com',
      password: 'Test1234!'
    },

    // Feature flags
    enableLoginTests: true,
    enableApiTests: true,
    enableE2ETests: true,
  }
});
