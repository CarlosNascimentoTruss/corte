package br.com.sankhya.truss.liberacaovencimento;

import java.math.BigDecimal;
import java.util.Collection;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;

public class ExcluiLibAction implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		
		JapeWrapper traslibDAO = JapeFactory.dao("AD_TRASLIBVCTOPES");
		JapeWrapper apoDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_APONTAMENTO);
		JapeWrapper libDAO = JapeFactory.dao(DynamicEntityNames.LIBERACAO_LIMITE);
		
		Registro[] linhas = ctx.getLinhas();
		
		for(Registro linha : linhas) {
			
			BigDecimal idiatv = apoDAO.findByPK(linha.getCampo("NUAPO")).asBigDecimal("IDIATV");
			
			Collection<DynamicVO> libsVO = traslibDAO.find("IDIATV = ? AND CODPRODMP = ? AND SITUACAO = 'P'", idiatv, linha.getCampo("CODPRODMP"));
			
			for(DynamicVO libVO : libsVO) {
				libDAO.deleteByCriteria("TABELA = 'TPRIATV' AND NUCHAVE = ? AND SEQUENCIA = ?", idiatv, libVO.asBigDecimal("SEQLIB"));
				
				traslibDAO.prepareToUpdate(libVO)
				.set("SITUACAO", "E")
				.update();
			}
			
			ctx.setMensagemRetorno(libsVO.size() + " Liberações Excluídas.");
		}
		
		
		
		
		
		
	}

}
