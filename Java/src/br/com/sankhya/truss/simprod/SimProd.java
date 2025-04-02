package br.com.sankhya.truss.simprod;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.ResultSet;

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

public class SimProd {

	public void simulaProducao(BigDecimal codprod, BigDecimal lotePad, BigDecimal nuupl) throws MGEModelException {
		JapeWrapper simiteDAO = JapeFactory.dao("AD_SIMPRODITE");
		JapeWrapper simprodDAO = JapeFactory.dao("AD_SIMPROD");
		JapeWrapper proDAO = JapeFactory.dao(DynamicEntityNames.PRODUTO);
		JapeWrapper estDAO = JapeFactory.dao("AD_CONFESTSIMPROD");
		SessionHandle hnd = null;
		JdbcWrapper jdbc = null;
		DynamicVO proVO = null;
		
		try {
			hnd = JapeSession.open();
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			jdbc = dwfFacade.getJdbcWrapper();
			jdbc.openSession();
				
				
			NativeSql q = new NativeSql(jdbc);
			ResultSet r = null;
			
			r = q.executeQuery("SELECT VW.PRODUTO, VW.CODPRODMP, SUM(VW.QTDMISTURA) AS QTDMISTURA, VW.CODVOL FROM AD_ARVOREPRODUTO VW JOIN TPRPRC PRC ON PRC.CODPRC = VW.CODPRC AND PRC.VERSAO = VW.VERSAO AND PRC.PADRAO = 'S' WHERE PRODUTO = " + codprod + " GROUP BY VW.PRODUTO, VW.CODPRODMP, VW.CODVOL");
			
			DynamicVO simprodVO = simprodDAO.create()
			.set("CODPRODPA", codprod)
			.set("TAMLOTEPAD", lotePad)
			.set("NUUPL", nuupl)
			.save();
			
			BigDecimal nusim = simprodVO.asBigDecimal("NUSIM");
			
			
			while(r.next()){
					NativeSql query = new NativeSql(jdbc);
					ResultSet rs = null;
					
					proVO = proDAO.findByPK(r.getBigDecimal("CODPRODMP"));
					
					NativeSql q2 = new NativeSql(jdbc);
					ResultSet r2 = null;
					
					BigDecimal codemp = estDAO.findByPK(BigDecimal.ONE).asBigDecimal("CODEMP");
					BigDecimal codlocal = estDAO.findByPK(BigDecimal.ONE).asBigDecimal("CODLOCAL");
					
					r2 = q2.executeQuery("SELECT SUM(ESTOQUE - RESERVADO) AS ESTOQUE\r\n" + 
							"					FROM TGFEST\r\n" + 
							"					WHERE CODEMP = " + codemp + " " + 
							"					AND CODLOCAL = " + codlocal + " " +
							"					AND CODPROD = " + r.getBigDecimal("CODPRODMP") + 
							"                   AND NVL(CODPARC,0) = 0");
					
					BigDecimal estoque = BigDecimal.ZERO;
					
					if(r2.next()) {
						estoque = r2.getBigDecimal("ESTOQUE");
					}
					
					BigDecimal estmax = proVO.asBigDecimalOrZero("ESTMAX");
					BigDecimal estmin = proVO.asBigDecimalOrZero("ESTMIN");
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
					.set("NUSIM", nusim)
					.set("CODPROD", r.getBigDecimal("CODPRODMP"))
					.set("QTDMISTURA", r.getBigDecimal("QTDMISTURA").multiply(lotePad, new MathContext(6)))
					.set("CODVOL", r.getString("CODVOL"))
					.set("ESTMAX", estmax)
					.set("ESTMIN", estmin)
					.set("ESTOQUE", estoque)
					.set("QTDPEDCOMPRA", qtdpedido)
					
					.save();
					
					
			}

		} catch(Exception e) {
			e.printStackTrace();
			MGEModelException.throwMe(new Exception(e.getMessage()));
		} finally {
			JdbcWrapper.closeSession(jdbc);
			JapeSession.close(hnd);
		}
	}
	
	
	
}
