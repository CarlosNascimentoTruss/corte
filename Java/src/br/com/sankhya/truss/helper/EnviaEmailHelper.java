package br.com.sankhya.dctm.helper;

import java.math.BigDecimal;
import java.sql.Timestamp;

import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.modelcore.util.DynamicEntityNames;

public class EnviaEmailHelper {

	public void insereEmail(
			char[] mensagem,
			BigDecimal maxtentenvio,
			String assunto,
			String email,
			BigDecimal codusu
			) throws Exception {
		FluidCreateVO mensagemVO = JapeFactory.dao(DynamicEntityNames.FILA_MSG).create();
		mensagemVO.set("CODFILA",getCodFila());
		mensagemVO.set("DTENTRADA",new Timestamp(System.currentTimeMillis()));
		mensagemVO.set("MENSAGEM",mensagem);
		mensagemVO.set("TIPOENVIO","E");
		mensagemVO.set("MAXTENTENVIO",maxtentenvio);
		mensagemVO.set("ASSUNTO",assunto);
		mensagemVO.set("EMAIL",email);
		mensagemVO.set("MIMETYPE","text/html" );
		mensagemVO.set("CODUSU",codusu);
		mensagemVO.set("STATUS", "Pendente");
		mensagemVO.set("CODCON", BigDecimal.ZERO);
		mensagemVO.set("TENTENVIO", BigDecimal.ZERO);
		mensagemVO.set("REENVIAR", "N");
		mensagemVO.save();
		
		atualizaTGFNUM(getCodFila());
	}

	private void atualizaTGFNUM(BigDecimal codFila) throws Exception {
		JapeWrapper dao = JapeFactory.dao(DynamicEntityNames.CONTROLE_NUMERACAO);
		DynamicVO registroTGFNUM = dao.findOne(" ARQUIVO = 'TMDFMG'");
		dao.prepareToUpdate(registroTGFNUM).set("ULTCOD", codFila).update();
	}

	private BigDecimal getCodFila() throws Exception {
		DynamicVO registroTGFNUM = JapeFactory.dao(DynamicEntityNames.CONTROLE_NUMERACAO).findOne(" ARQUIVO = 'TMDFMG'"); 
		BigDecimal ultimoCod = registroTGFNUM.asBigDecimal("ULTCOD");
		return ultimoCod.add(BigDecimal.ONE);
	}
}
