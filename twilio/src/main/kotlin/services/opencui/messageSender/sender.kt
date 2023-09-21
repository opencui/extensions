import io.opencui.core.IService

public interface IMessageSender : IService {
  @JsonIgnore
  public fun send(phoneNumer: String, content: String): Boolean
}