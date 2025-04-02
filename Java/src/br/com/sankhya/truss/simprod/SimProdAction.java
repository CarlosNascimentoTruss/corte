package br.com.sankhya.truss.simprod;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.ResultSet;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class SimProdAction implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		JapeWrapper simiteDAO = JapeFactory.dao("AD_SIMPRODITE");
		JapeWrapper proDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
		SessionHandle hnd = null;
		JdbcWrapper jdbc = null;
		DynamicVO proVO = null;
		
		try {
			hnd = JapeSession.open();
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			jdbc = dwfFacade.getJdbcWrapper();
			jdbc.openSession();
			
			Registro[] linhas = ctx.getLinhas();
			
			
			
			for(Registro linha: linhas) {
				
				BigDecimal lotepad = (BigDecimal) linha.getCampo("TAMLOTEPAD");
						
				NativeSql q = new NativeSql(jdbc);
				ResultSet r = null;
				
				r = q.executeQuery("SELECT VW.PRODUTO, VW.CODPRODMP, SUM(VW.QTDMISTURA) AS QTDMISTURA, VW.CODVOL FROM AD_ARVOREPRODUTO VW JOIN TPRPRC PRC ON PRC.CODPRC = VW.CODPRC AND PRC.VERSAO = VW.VERSAO AND PRC.PADRAO = 'S' WHERE PRODUTO = " + (BigDecimal) linha.getCampo("CODPRODPA") + " GROUP BY VW.PRODUTO, VW.CODPRODMP, VW.CODVOL");
				
				
				while(r.next()){
					NativeSql query = new NativeSql(jdbc);
					ResultSet rs = null;
					
					proVO = proDAO.findByPK(r.getBigDecimal("CODPRODMP"));
					
					NativeSql q2 = new NativeSql(jdbc);
					ResultSet r2 = null;

					r2 = q2.executeQuery("SELECT SUM(ESTOQUE - RESERVADO) AS ESTOQUE\r\n" + 
							"					FROM TGFEST\r\n" + 
							"					WHERE CODEMP = 6\r\n" + 
							"					AND CODLOCAL = 11100" + 
							"					AND CODPARC = 0" + 
							"					AND CODPROD = " + r.getBigDecimal("CODPRODMP"));
					
					BigDecimal estoque = BigDecimal.ZERO;
					
					if(r2.next()) {
						estoque = r2.getBigDecimal("ESTOQUE");
					}
					
					BigDecimal estmax = proVO.asBigDecimal("ESTMAX");
					BigDecimal estmin = proVO.asBigDecimal("ESTMIN");
					BigDecimal qtdpedido = BigDecimal.ZERO;
					
					rs = query.executeQuery("SELECT NVL(SUM(ITE.QTDNEG),0) AS QUANTIDADE\r\n" + 
							"FROM TGFITE ITE\r\n" + 
							"JOIN TGFCAB CAB ON CAB.NUNOTA = ITE.NUNOTA\r\n" + 
							"WHERE CAB.TIPMOV = 'O'\r\n" + 
							"AND CAB.STATUSNOTA = 'L'\r\n" + 
							"AND ITE.PENDENTE = 'S'\r\n" + 
							"AND ITE.CODPROD = " + r.getBigDecimal("CODPRODMP"));
					
					if(rs.next()) {
						qtdpedido = rs.getBigDecimal("QUANTIDADE");
					}
					
					simiteDAO.create()
					.set("NUSIM", (BigDecimal) linha.getCampo("NUSIM"))
					.set("CODPROD", r.getBigDecimal("CODPRODMP"))
					.set("QTDMISTURA", r.getBigDecimal("QTDMISTURA").multiply(lotepad, new MathContext(6)))
					.set("CODVOL", r.getString("CODVOL"))
					.set("ESTMAX", estmax)
					.set("ESTMIN", estmin)
					.set("ESTOQUE", estoque)
					.set("QTDPEDCOMPRA", qtdpedido)
					.save();
					
					
				}
			}
			ctx.setMensagemRetorno("Simulação Concluída.");
		} catch(Exception e) {
			MGEModelException.throwMe(new Exception(e.getMessage()));
		} finally {
			JdbcWrapper.closeSession(jdbc);
			JapeSession.close(hnd);
		}
	}
}







