package me.quickstart.helloworldService

import com.fasterxml.jackson.annotation.JsonIgnore
import io.opencui.core.Configuration
import io.opencui.core.ExtensionBuilder
import io.opencui.core.IProvider
import io.opencui.core.IService
import io.opencui.core.UserSession
import kotlin.String

public interface IHelloworldService : IService {
  @JsonIgnore
  public fun testFunction(str: String): String?
}

// The first native implementation.
data class HelloWorldProvider(
  val config: Configuration,
  override var session: UserSession? = null): IHelloworldService, IProvider {

  override fun testFunction(str: String): String? {
    return "hello ${config["name"]}, $str"
  }

  companion object: ExtensionBuilder {
    override fun invoke(config: Configuration): IHelloworldService {
      return HelloWorldProvider(config)
    }
  }
}