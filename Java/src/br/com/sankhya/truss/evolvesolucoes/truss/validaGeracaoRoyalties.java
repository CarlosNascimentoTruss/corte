package br.com.evolvesolucoes.truss;

import java.math.BigDecimal;
import java.util.Collection;

import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;

public class validaGeracaoRoyalties {
	
	
	private Collection<DynamicVO> getItesVO(BigDecimal nunota) throws Exception {
		JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
		Collection<DynamicVO> itesVO = iteDAO.find("NUNOTA = ? AND NVL(AD_CALCSERVICO,'N') = 'N'",nunota);
		return itesVO;
	}
	
	public BigDecimal qtdItensGeraRoyalties(BigDecimal nunota) throws Exception {
		Collection<DynamicVO> itesVO = getItesVO(nunota);
		return new BigDecimal(itesVO.size());
	}
	
	public BigDecimal getVlrItensRoyalties(BigDecimal nunota) throws Exception {
		Collection<DynamicVO> itesVO = getItesVO(nunota);
		BigDecimal vlrItens = BigDecimal.ZERO;
		for(DynamicVO iteVO : itesVO) {
			vlrItens = vlrItens.add(iteVO.asBigDecimal("VLRTOT"));
		}
		return vlrItens;
	}
	
	public boolean pedidoFoiFaturado(BigDecimal nunota) throws Exception {
		JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
		DynamicVO iteVO = iteDAO.findOne("NUNOTA = ? AND QTDENTREGUE > 0", nunota);
		
		if(iteVO == null) {
			return false;
		} else {
			return true;
		}
	}
	
	public void atualizaItens(BigDecimal nunota) throws Exception {
		Collection<DynamicVO> itesVO = getItesVO(nunota);
		JapeWrapper iteDAO = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA);
		
		for(DynamicVO iteVO : itesVO) {
			iteDAO.prepareToUpdate(iteVO)
			.set("AD_CALCSERVICO", "S")
			.update();
		}
		
		
	}
	
}