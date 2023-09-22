package services.opencui.messageSender

import io.opencui.core.IService


import com.fasterxml.jackson.annotation.JsonIgnore
import io.opencui.core.*
public interface IMessageSender : IService {
  @JsonIgnore
  public fun send(phoneNumer: String, content: String): Unit
}