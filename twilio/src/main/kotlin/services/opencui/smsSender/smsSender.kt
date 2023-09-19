import io.opencui.core.IService

public interface ISmsSender : IService {
  @JsonIgnore
  public fun sendSms(phoneNumer: String, content: String): Boolean
}