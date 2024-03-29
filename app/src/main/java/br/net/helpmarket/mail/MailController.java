package br.net.helpmarket.mail;

import android.content.Context;

import br.net.helpmarket.modelo.Usuario;

public class MailController {

    private Context context;
    private final String HOST = "suporte.helpmarket@gmail.com";
    private final String PASS = "soeusei01";
    private final String BOTTOM = "\n\n\n HelpMarket - Dê um help nas suas compras.";

    public MailController(Context context) {
        this.context = context;
    }

    public void enviarCodigoRecuperaco(Usuario usuario, int codigo) {
        BackgroundMail bm = new BackgroundMail(context);
        bm.setGmailUserName(HOST);
        bm.setGmailPassword(PASS);
        bm.setMailTo(usuario.getEmail());
        bm.setFormSubject("NO-REPLY - CÓDIGO DE RECUPERAÇÃO");
        bm.setFormBody("Olá " + usuario.getNome() + ", você solicitou um código para recuperação de conta.\n\nSeu código é: " + codigo + BOTTOM);
        bm.setSendingMessage("Enviando código de recuperação para: " + usuario.getEmail());
        bm.setSendingMessageSuccess("Enviado com sucesso!");
        bm.setProcessVisibility(true);
        bm.send();
    }

    public void enviarAlteracaoSenha(Usuario usuario) {
        BackgroundMail bm = new BackgroundMail(context);
        bm.setGmailUserName(HOST);
        bm.setGmailPassword(PASS);
        bm.setMailTo(usuario.getEmail());
        bm.setFormSubject("NO-REPLY - ALTERAÇÃO DE SENHA");
        bm.setFormBody("Olá " + usuario.getNome() + ", a senha da sua conta foi alterada recentemente, foi você? Se não tiver sido você, acesse o aplicativo e recupere sua conta utilizando a opção esqueci minha senha na tela de login." + BOTTOM);
        bm.setSendingMessage("Enviando...");
        bm.setSendingMessageSuccess("E-mail enviado com sucesso!");
        bm.setProcessVisibility(false);
        bm.send();
    }

    public void enviarMensagemBoasVindas(Usuario usuario) {
        BackgroundMail bm = new BackgroundMail(context);
        bm.setGmailUserName(HOST);
        bm.setGmailPassword(PASS);
        bm.setMailTo(usuario.getEmail());
        bm.setFormSubject("NO-REPLY - SEJA BEM-VINDO");
        bm.setFormBody("Olá " + usuario.getNome() + ", seja bem-vindo ao HelpMarket, utilize a aba Ajuda e Informações para um guia basico de uso e tirar algumas duvidas sobre o aplicativo." + BOTTOM);
        bm.setSendingMessage("Enviando...");
        bm.setSendingMessageSuccess("E-mail enviado com sucesso!");
        bm.setProcessVisibility(false);
        bm.send();
    }

    public void enviarMensagemSuporte(String nome, String email, String motivo, String titulo, String msg) {
        BackgroundMail bm = new BackgroundMail(context);
        bm.setGmailUserName(HOST);
        bm.setGmailPassword(PASS);
        bm.setMailTo("duvidas.helpmarket@gmail.com");
        bm.setFormSubject(motivo + " - " + titulo);
        bm.setFormBody("Nome: " + nome + "\nE-mail: " + email + "\n\nMotivo: " + motivo + "\n\nTitulo: " + titulo + "\nMensagem: " + msg);
        bm.setSendingMessage("Enviando mensagem para o suporte");
        bm.setSendingMessageSuccess("Enviado!");
        bm.setProcessVisibility(true);
        //Enviar anexo: bm.setAttachment(Environment.getExternalStorageDirectory().getPath()+File.pathSeparator+"somefile.txt");
        bm.send();
    }
}
