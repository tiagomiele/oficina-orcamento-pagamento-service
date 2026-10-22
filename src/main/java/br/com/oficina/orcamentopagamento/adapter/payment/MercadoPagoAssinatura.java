package br.com.oficina.orcamentopagamento.adapter.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Valida o header {@code x-signature} do webhook do Mercado Pago: HMAC-SHA256 do manifesto {@code
 * id:<data.id>;request-id:<x-request-id>;ts:<ts>;} com a assinatura secreta do webhook.
 */
@Component
public class MercadoPagoAssinatura {

  private final String segredo;

  public MercadoPagoAssinatura(MercadoPagoProperties properties) {
    this.segredo = properties.webhookSecret();
  }

  public boolean valida(String assinatura, String requestId, String dataId) {
    if (segredo == null || segredo.isBlank() || assinatura == null || dataId == null) {
      return false;
    }
    String ts = null;
    String v1 = null;
    for (String parte : assinatura.split(",")) {
      String[] chaveValor = parte.trim().split("=", 2);
      if (chaveValor.length == 2 && chaveValor[0].equals("ts")) {
        ts = chaveValor[1];
      } else if (chaveValor.length == 2 && chaveValor[0].equals("v1")) {
        v1 = chaveValor[1];
      }
    }
    if (ts == null || v1 == null) {
      return false;
    }
    String id = dataId.matches("[A-Za-z0-9]+") ? dataId.toLowerCase() : dataId;
    StringBuilder manifesto = new StringBuilder("id:").append(id).append(';');
    if (requestId != null && !requestId.isBlank()) {
      manifesto.append("request-id:").append(requestId).append(';');
    }
    manifesto.append("ts:").append(ts).append(';');
    return MessageDigest.isEqual(
        hmac(manifesto.toString()).getBytes(StandardCharsets.UTF_8),
        v1.getBytes(StandardCharsets.UTF_8));
  }

  public String hmac(String manifesto) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(manifesto.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Falha ao calcular a assinatura do webhook", e);
    }
  }
}
