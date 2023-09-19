import io.opencui.core.IService
import io.opencui.core.PhoneNumber


public interface ISmsSender : IService {
  @JsonIgnore
  public fun sendSms(phoneNumer: PhoneNumber, content: String): Boolean
}