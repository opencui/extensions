package me.quickstart.helloworldService

import com.fasterxml.jackson.`annotation`.JsonIgnore
import io.opencui.core.*
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

  companion object: ExtensionBuilder<IHelloworldService> {
    override fun invoke(config: Configuration): IHelloworldService {
      return HelloWorldProvider(config)
    }
  }
}