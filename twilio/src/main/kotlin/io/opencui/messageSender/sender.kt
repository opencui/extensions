package io.opencui.messageSender

import io.opencui.core.IService

public interface IMessageSender : IService {
  fun send(phoneNumber: String, content: String)
}