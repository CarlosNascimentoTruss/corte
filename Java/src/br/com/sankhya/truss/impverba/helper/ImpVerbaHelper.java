package br.com.sankhya.truss.impverba.helper;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.truss.impverba.model.ImpVerbaModel;

import java.math.BigDecimal;
import java.util.Collection;

/* 
 * Rotina que realiza a importação de um csv com um determinado template para a tela controle de verbas.
 * Código para atendimento do seguinte card: https://grupoboticario.kanbanize.com/ctrl_board/301/cards/1578963/details/
 * 
 * */

public class ImpVerbaHelper {
	
	
	public void insereVerbas(Collection<ImpVerbaModel> verbas, BigDecimal codusu, BigDecimal idImportacao) throws Exception {
		JapeWrapper parDAO = JapeFactory.dao(DynamicEntityNames.PARCEIRO);
		JapeWrapper verbasDAO = JapeFactory.dao("AD_CONTROLEVERBAS");
		JapeWrapper natDAO = JapeFactory.dao(DynamicEntityNames.NATUREZA);
		JapeWrapper empDAO = JapeFactory.dao(DynamicEntityNames.EMPRESA);
		
		JdbcWrapper jdbc = null;
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		jdbc = dwfFacade.getJdbcWrapper();
		jdbc.openSession();
		
		// Obtém o máximo número único para gerar um sequencial
		BigDecimal maxNunico = NativeSql.getBigDecimal("MAX(TO_NUMBER(NUNICO))", "AD_CONTROLEVERBAS", "1 = 1");
		
		// Loop nos odelos de verba passado como parâmetro
		for(ImpVerbaModel verba : verbas) {
			maxNunico = maxNunico.add(BigDecimal.ONE);

			String cnpjFormatado = String.format("%14s", verba.getCnpjParc()).replace(' ', '0');
			DynamicVO parVO = parDAO.findOne("CGC_CPF = ?", cnpjFormatado);
			DynamicVO natVO = natDAO.findByPK(verba.getCodnat());
			DynamicVO empVO = empDAO.findByPK(verba.getCodemp());
			
			// Valida se existe parceiro, natureza e empresa informados na planilha
			if(parVO == null) {
				throw new Exception("Não foi possível importar verbas. Não foi localizado nenhum parceiro ativo com o CNPJ " + verba.getCnpjParc());
			} else {
				if (!"S".equals(parVO.asString("ATIVO"))) {
					throw new Exception("Não foi possível importar verbas. O parceiro com o CNPJ " + verba.getCnpjParc() + " não se encontra ativo.");
				}
			}
			
			if(natVO == null) {
				throw new Exception("Não foi possível importar verbas. Não foi localizado nenhuma natureza com o código " + verba.getCodnat());
			}
			
			if(empVO == null) {
				throw new Exception("Não foi possível importar verbas. Não foi localizado nenhuma empresa com o código " + verba.getCodemp());
			}
			
			// cria registro das verbas
			verbasDAO.create()
			.set("NUNICO", maxNunico.toString())
			.set("DTINCLUSAO", verba.getDtInclusao())
			.set("VLRVERBA", verba.getVlrInicialVerba())
			.set("SALDOVERBA", verba.getVlrInicialVerba())
			.set("TIPOVERBAS", "S")
			.set("CODPARC", parVO.asBigDecimal("CODPARC"))
			.set("CODEMP", verba.getCodemp())
			.set("CODNAT", verba.getCodnat())
			.set("CODUSU", codusu.toString())
			.set("DTINICIAL", verba.getDtInicial())
			.set("DTFINAL", verba.getDtFinal())
			.set("CNPJPARC", verba.getCnpjParc())
			.set("CNPJEMP", empVO.asString("CGC"))
			.set("RAZAO", parVO.asString("RAZAOSOCIAL"))
			.set("INTEGMERCANET", "N")
			.set("INTEGSANKHYA", "N")
			.set("IDIMPORTACAO", idImportacao)
			.set("AD_IDEXTERNO", idImportacao)
			.save();
			
		}
	}
}




