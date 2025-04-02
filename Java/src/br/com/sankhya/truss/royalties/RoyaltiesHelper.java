package br.com.sankhya.truss.royalties;

import java.math.BigDecimal;

import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class RoyaltiesHelper {
	
	
	public boolean topGeraRoyalties(BigDecimal codtipoper) throws Exception {
		
		JapeWrapper centParamDAO = JapeFactory.dao("AD_CENTRALPARAMTOP");
		
		DynamicVO centralVOs = centParamDAO.findOne("CODTIPOPER = ? AND NUPAR = 4", codtipoper);
		
		return centralVOs != null;
	}
	
	
	
	
}