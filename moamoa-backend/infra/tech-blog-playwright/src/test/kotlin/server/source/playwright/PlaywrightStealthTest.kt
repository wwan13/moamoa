package server.source.playwright

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import test.UnitTest

class PlaywrightStealthTest : UnitTest() {

    @Test
    fun `chromium 자동화 감지 완화 옵션을 사용한다`() {
        PlaywrightStealth.launchArgs shouldContain "--disable-blink-features=AutomationControlled"
    }

    @Test
    fun `브라우저 컨텍스트에 자동화 감지 완화 스크립트를 주입한다`() {
        PlaywrightStealth.initScript shouldContain "navigator, 'webdriver'"
        PlaywrightStealth.initScript shouldContain "navigator, 'languages'"
        PlaywrightStealth.initScript shouldContain "navigator, 'plugins'"
        PlaywrightStealth.initScript shouldContain "window.chrome.runtime"
        PlaywrightStealth.initScript shouldContain "window.navigator.permissions.query"
    }
}
