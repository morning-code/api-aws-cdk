package io.code.morning

import software.amazon.awscdk.core.App
import software.amazon.awscdk.core.Environment
import software.amazon.awscdk.core.StackProps

fun main() {
  val app = App()
  ApiStack(
    app, "morning-code-api",
    StackProps.builder()
      .env(
        Environment.builder()
          .region("ap-northeast-1")
          .build()
      )
      .build()
  )

  app.synth()

}
