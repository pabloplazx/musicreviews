package com.musicreviews.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.url}")
    private String appUrl;

    @Value("${app.mail.from}")
    private String fromEmail;

    public void enviarConfirmacion(String emailDestino, String username, String token) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(emailDestino);
            helper.setSubject("Confirma tu cuenta en MusicReviews");
            helper.setText(buildHtml(username, appUrl + "/verificar?token=" + token), true);

            mailSender.send(mensaje);
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar el email de confirmación: " + e.getMessage());
        }
    }

    public void enviarRestablecimiento(String emailDestino, String username, String token) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(emailDestino);
            helper.setSubject("Restablece tu contraseña en MusicReviews");
            helper.setText(buildHtmlReset(username, appUrl + "/reset-password?token=" + token), true);

            mailSender.send(mensaje);
        } catch (Exception e) {
            throw new RuntimeException("Error al enviar el email de restablecimiento: " + e.getMessage());
        }
    }

    private String buildHtmlReset(String username, String url) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background:#080d0a;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#080d0a;padding:40px 16px;">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0" style="max-width:560px;width:100%%;">

                    <!-- HEADER -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#1a3a2a 0%%,#0f2018 100%%);border-radius:20px 20px 0 0;padding:40px 48px;text-align:center;border:1px solid #243d2e;border-bottom:none;">
                        <div style="display:inline-block;background:linear-gradient(135deg,#48a377,#2d6e50);border-radius:50%%;width:56px;height:56px;line-height:56px;text-align:center;font-size:26px;margin-bottom:16px;box-shadow:0 0 30px rgba(72,163,119,0.4);">🔑</div>
                        <p style="margin:0;font-size:26px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;">MusicReviews</p>
                        <p style="margin:8px 0 0;font-size:11px;font-weight:600;color:rgba(255,255,255,0.45);text-transform:uppercase;letter-spacing:3px;">Restablecer contraseña</p>
                      </td>
                    </tr>

                    <!-- BODY -->
                    <tr>
                      <td style="background:#0f1a13;padding:44px 48px 36px;border-left:1px solid #243d2e;border-right:1px solid #243d2e;">
                        <p style="margin:0 0 6px;font-size:22px;font-weight:700;color:#e4ede8;">Hola, <span style="color:#48a377;">%s</span></p>
                        <p style="margin:0 0 32px;font-size:15px;color:#7a9485;line-height:1.75;">Hemos recibido una solicitud para restablecer la contraseña de tu cuenta. Pulsa el botón de abajo para elegir una nueva. El enlace es válido durante <strong style="color:#5a7a65;">30 minutos</strong>.</p>

                        <!-- CTA BUTTON -->
                        <table width="100%%" cellpadding="0" cellspacing="0">
                          <tr>
                            <td align="center" style="padding:8px 0 32px;">
                              <a href="%s" style="display:inline-block;background:linear-gradient(135deg,#48a377 0%%,#2d6e50 100%%);color:#ffffff;font-size:15px;font-weight:700;padding:16px 44px;border-radius:50px;text-decoration:none;letter-spacing:0.4px;box-shadow:0 4px 28px rgba(72,163,119,0.45);">
                                🔑 &nbsp;Restablecer contraseña
                              </a>
                            </td>
                          </tr>
                        </table>

                        <!-- DIVIDER -->
                        <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:28px;">
                          <tr>
                            <td style="border-top:1px solid #1e3028;"></td>
                            <td style="white-space:nowrap;padding:0 16px;font-size:12px;color:#3d5a48;">o copia el enlace en tu navegador</td>
                            <td style="border-top:1px solid #1e3028;"></td>
                          </tr>
                        </table>
                        <p style="margin:0 0 28px;font-size:11px;color:#3a5244;word-break:break-all;background:#0a1410;border:1px solid #1a2e22;border-radius:8px;padding:12px 16px;">%s</p>

                        <!-- WARNING BOX -->
                        <div style="background:#0a1410;border:1px solid #1a2e22;border-left:3px solid #2d6e50;border-radius:0 10px 10px 0;padding:14px 18px;">
                          <p style="margin:0;font-size:12px;color:#4a6455;line-height:1.65;">Si no solicitaste este cambio, ignora este mensaje. Tu contraseña actual seguirá siendo la misma. El enlace expirará automáticamente en <strong style="color:#5a7a65;">30 minutos</strong>.</p>
                        </div>
                      </td>
                    </tr>

                    <!-- FOOTER -->
                    <tr>
                      <td style="background:#0a1410;border-radius:0 0 20px 20px;padding:24px 48px;text-align:center;border:1px solid #243d2e;border-top:none;">
                        <p style="margin:0 0 4px;font-size:13px;font-weight:600;color:#48a377;">♪ MusicReviews</p>
                        <p style="margin:0;font-size:11px;color:#2d4a3a;">Tu diario de música y reseñas · zentimes.es</p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(username, url, url);
    }

    private String buildHtml(String username, String url) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background:#080d0a;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#080d0a;padding:40px 16px;">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0" style="max-width:560px;width:100%%;">

                    <!-- HEADER -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#1a3a2a 0%%,#0f2018 100%%);border-radius:20px 20px 0 0;padding:40px 48px;text-align:center;border:1px solid #243d2e;border-bottom:none;">
                        <div style="display:inline-block;background:linear-gradient(135deg,#48a377,#2d6e50);border-radius:50%%;width:56px;height:56px;line-height:56px;text-align:center;font-size:26px;margin-bottom:16px;box-shadow:0 0 30px rgba(72,163,119,0.4);">♪</div>
                        <p style="margin:0;font-size:26px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;">MusicReviews</p>
                        <p style="margin:8px 0 0;font-size:11px;font-weight:600;color:rgba(255,255,255,0.45);text-transform:uppercase;letter-spacing:3px;">Verificación de cuenta</p>
                      </td>
                    </tr>

                    <!-- BODY -->
                    <tr>
                      <td style="background:#0f1a13;padding:44px 48px 36px;border-left:1px solid #243d2e;border-right:1px solid #243d2e;">
                        <p style="margin:0 0 6px;font-size:22px;font-weight:700;color:#e4ede8;">Hola, <span style="color:#48a377;">%s</span> 👋</p>
                        <p style="margin:0 0 32px;font-size:15px;color:#7a9485;line-height:1.75;">Bienvenido a MusicReviews, tu espacio para descubrir, reseñar y compartir música. Un último paso antes de empezar: confirma tu dirección de email.</p>

                        <!-- CTA BUTTON -->
                        <table width="100%%" cellpadding="0" cellspacing="0">
                          <tr>
                            <td align="center" style="padding:8px 0 32px;">
                              <a href="%s" style="display:inline-block;background:linear-gradient(135deg,#48a377 0%%,#2d6e50 100%%);color:#ffffff;font-size:15px;font-weight:700;padding:16px 44px;border-radius:50px;text-decoration:none;letter-spacing:0.4px;box-shadow:0 4px 28px rgba(72,163,119,0.45);">
                                ✓ &nbsp;Confirmar mi cuenta
                              </a>
                            </td>
                          </tr>
                        </table>

                        <!-- DIVIDER -->
                        <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:28px;">
                          <tr>
                            <td style="border-top:1px solid #1e3028;"></td>
                            <td style="white-space:nowrap;padding:0 16px;font-size:12px;color:#3d5a48;">o copia el enlace en tu navegador</td>
                            <td style="border-top:1px solid #1e3028;"></td>
                          </tr>
                        </table>
                        <p style="margin:0 0 28px;font-size:11px;color:#3a5244;word-break:break-all;background:#0a1410;border:1px solid #1a2e22;border-radius:8px;padding:12px 16px;">%s</p>

                        <!-- WARNING BOX -->
                        <div style="background:#0a1410;border:1px solid #1a2e22;border-left:3px solid #2d6e50;border-radius:0 10px 10px 0;padding:14px 18px;">
                          <p style="margin:0;font-size:12px;color:#4a6455;line-height:1.65;">Si no creaste esta cuenta en MusicReviews, puedes ignorar este mensaje con total seguridad. El enlace expira en <strong style="color:#5a7a65;">24 horas</strong>.</p>
                        </div>
                      </td>
                    </tr>

                    <!-- FOOTER -->
                    <tr>
                      <td style="background:#0a1410;border-radius:0 0 20px 20px;padding:24px 48px;text-align:center;border:1px solid #243d2e;border-top:none;">
                        <p style="margin:0 0 4px;font-size:13px;font-weight:600;color:#48a377;">♪ MusicReviews</p>
                        <p style="margin:0;font-size:11px;color:#2d4a3a;">Tu diario de música y reseñas · zentimes.es</p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(username, url, url);
    }
}
