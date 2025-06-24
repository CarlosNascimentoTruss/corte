package br.com.sankhya.truss.corte.depara.actions;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;

import java.math.BigDecimal;

public class DeParaTemp implements AcaoRotinaJava {
    @Override
    public void doAction(ContextoAcao ctx) throws Exception {
        BigDecimal usuarioLogado = ctx.getUsuarioLogado();
        for (Registro linha : ctx.getLinhas()) {
            BigDecimal nunota = (BigDecimal) linha.getCampo("NUNOTA");
            DePara dePara = new DePara();
            dePara.acao(nunota,usuarioLogado);
        }
    }
}
