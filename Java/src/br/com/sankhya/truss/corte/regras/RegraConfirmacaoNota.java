package br.com.sankhya.truss.corte.regras;

import br.com.sankhya.extensions.regrasnegocio.ContextoRegra;
import br.com.sankhya.extensions.regrasnegocio.RegraNegocioJava;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.truss.corte.helper.CorteHelper;

public class RegraConfirmacaoNota implements RegraNegocioJava {


    @Override
    public void executa(ContextoRegra ctx) throws Exception {


        JapeWrapper parDAO = JapeFactory.dao(DynamicEntityNames.PARCEIRO);
        JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
        DynamicVO cabVO = cabDAO.findByPK(ctx.getNunota());

        try {
            String separaTerceiros = parDAO.findByPK(cabVO.asBigDecimal("CODPARC")).asString("AD_LOCALSEPARACAO");
            separaTerceiros = separaTerceiros == null ? "1" : separaTerceiros;
            String indicaLote = CorteHelper.validaTopIndicaLote(cabVO.asBigDecimal("CODTIPOPER"));

            if ("2".equals(separaTerceiros) && "S".equals(indicaLote)) {
                CorteHelper.indicaLotes(ctx.getNunota());
                cabDAO.prepareToUpdateByPK(ctx.getNunota())
                        .set("AD_STATUSPED", "28")
                        .update();

            }
            ctx.setSucesso(true);
        } catch(Exception e){
            e.printStackTrace();
            throw new Exception("Erro na regra de neg" + e.getMessage());
        }

    }

}
