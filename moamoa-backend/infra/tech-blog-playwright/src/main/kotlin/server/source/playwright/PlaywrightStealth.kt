package server.source.playwright

import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType

internal object PlaywrightStealth {

    internal val launchArgs = listOf(
        "--disable-blink-features=AutomationControlled",
    )

    internal val initScript =
        """
        (() => {
          Object.defineProperty(navigator, 'webdriver', {
            get: () => undefined,
          });

          Object.defineProperty(navigator, 'languages', {
            get: () => ['ko-KR', 'ko', 'en-US', 'en'],
          });

          Object.defineProperty(navigator, 'plugins', {
            get: () => [1, 2, 3, 4, 5],
          });

          window.chrome = window.chrome || {};
          window.chrome.runtime = window.chrome.runtime || {};

          const originalQuery = window.navigator.permissions?.query;
          if (originalQuery) {
            window.navigator.permissions.query = (parameters) => {
              if (parameters?.name === 'notifications') {
                return Promise.resolve({ state: Notification.permission });
              }
              return originalQuery(parameters);
            };
          }
        })();
        """.trimIndent()

    fun launchOptions(): BrowserType.LaunchOptions =
        BrowserType.LaunchOptions()
            .setHeadless(true)
            .setArgs(launchArgs)

    fun applyTo(context: BrowserContext) {
        context.addInitScript(initScript)
    }
}
