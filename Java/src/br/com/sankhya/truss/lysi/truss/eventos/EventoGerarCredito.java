package br.com.lysi.truss.eventos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class EventoGerarCredito implements EventoProgramavelJava{
	private static JdbcWrapper		jdbc;
	private static NativeSql		nativeSql 	= null;
	
	
	private void gerarCredito(PersistenceEvent event) throws Exception {
		try {
			
			openSession();
			
			DynamicVO linhaEvento = (DynamicVO) event.getVo();
			
			BigDecimal nuFin = linhaEvento.asBigDecimal("NUFIN");
			
			//só executa na baixa
			BigDecimal codTipOperBaixa = linhaEvento.asBigDecimalOrZero("CODTIPOPERBAIXA");
			if ( codTipOperBaixa.equals(BigDecimal.ZERO))
				return;
			BigDecimal vlrBaixa = linhaEvento.asBigDecimalOrZero("VLRBAIXA");
			if ( vlrBaixa.equals(BigDecimal.ZERO))
				return;
			
			BigDecimal recDesp = linhaEvento.asBigDecimalOrZero("RECDESP");
			if ( !recDesp.equals(BigDecimal.ONE) )
				return;
			
			//PARAMETROS
			String paramTopPgto = MGECoreParameter.getParameterAsString("TOPPAGTOAVISTA");
			if (paramTopPgto == null || paramTopPgto.equals("") )
				throw new Exception("Parâmetro TOPPAGTOAVISTA não foi configurado.");
			
			/*String paramNatPagto = MGECoreParameter.getParameterAsString("NATPAGTOAVISTA");
			if (paramNatPagto == null || paramNatPagto.equals("") )
				throw new Exception("Parâmetro NATPAGTOAVISTA não foi configurado.");
				*/
			
			String paramTipoTitulo = MGECoreParameter.getParameterAsString("TIPOAGTOAVISTA");
			if (paramTipoTitulo == null || paramTipoTitulo.equals("") )
				throw new Exception("Parâmetro TIPOAGTOAVISTA não foi configurado.");
			
			BigDecimal percentualDesc = (BigDecimal) MGECoreParameter.getParameter("DESCPAGTOAVISTA");
			if (percentualDesc == null || percentualDesc.equals(BigDecimal.ZERO) )
				throw new Exception("Parâmetro DESCPAGTOAVISTA não foi configurado.");
			
			BigDecimal codTipOper = linhaEvento.asBigDecimalOrZero("CODTIPOPER");
			//BigDecimal codNatureza = linhaEvento.asBigDecimalOrZero("CODNAT");
			BigDecimal codTipoTitulo = linhaEvento.asBigDecimalOrZero("CODTIPTIT");
			
			//se não tem a mesma top, natureza e tipo de titulo dos parametros, retorna
			if ( !codTipOperBaixa.equals(new BigDecimal(paramTopPgto) ) ||
				 //!codNatureza.equals(new BigDecimal(paramNatPagto) ) || 
				 !codTipoTitulo.equals(new BigDecimal(paramTipoTitulo) ) )
				 return;
			
			
			BigDecimal vlrCredito = vlrBaixa.multiply(percentualDesc).divide(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP);
			
			BigDecimal codParc = linhaEvento.asBigDecimalOrZero("CODPARC");
			JapeWrapper parceiroDAO = JapeFactory.dao(DynamicEntityNames.PARCEIRO);
			DynamicVO parceiroVO = parceiroDAO.findByPK(codParc);
			
			//se não está configurado nenhuma empresa, retorna
			if ( parceiroVO.asBigDecimalOrZero("AD_EMPROYALTIES").equals(BigDecimal.ZERO) && 
			     parceiroVO.asBigDecimalOrZero("AD_EMPTAXA").equals(BigDecimal.ZERO) )
				return;
					
			limparCreditoAnterior( nuFin );
			
			ResultSet rs = nativeSql.executeQuery( String.format( " SELECT * FROM AD_PAGTOAVISTA WHERE CODEMP IN ( %s, %s ) " , 	
					parceiroVO.asBigDecimalOrZero("AD_EMPROYALTIES"), 
					parceiroVO.asBigDecimalOrZero("AD_EMPTAXA") ) );
			while ( rs.next() ) {
				
				BigDecimal codEmp = rs.getBigDecimal("CODEMP"); 
				BigDecimal valor = vlrCredito.multiply( rs.getBigDecimal("PERCENTUAL") ).divide(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP);
				
				gerarCredito( nuFin, codParc, codEmp , valor );
				
			}
			rs.close();
			
		} catch (Exception e) {
			throw new Exception("Erro gerando crédito cliente pagamento à vista\n\n" + e.getMessage(), e);
		} finally {
			closeSession();
		}
	}
	
	private void gerarCredito( BigDecimal nuFin, BigDecimal codParc, BigDecimal codEmp, BigDecimal valor ) throws Exception {
		
		JapeWrapper creditoDAO = JapeFactory.dao("AD_CREDITOCLIENTE");
		creditoDAO.create()
			.set("CODPARC", codParc )
			.set("CODEMP", codEmp )
			.set("NUFIN", nuFin )
			.set("VLRCREDITO", valor )
			.save();
		
	}
	
	private void limparCreditoAnterior( BigDecimal nuFin ) throws Exception {
		
		JapeWrapper creditoDAO = JapeFactory.dao("AD_CREDITOCLIENTE");
		Collection<DynamicVO>  creditos = creditoDAO.find(" NUFIN = " + nuFin);
		
		for ( DynamicVO credito : creditos ) {
			
			ResultSet rs = nativeSql.executeQuery( String.format( " SELECT * FROM AD_CREDITOCONSUMO WHERE NUCREDITO = %s " , credito.asBigDecimal("NUCREDITO") ) );
			while ( rs.next() ) {
				throw new Exception("Não foi possível gerar novo crédito ao cliente, pois o crédito anterior deste financeiro já foi consumido. NUFIN: " + nuFin );
			}
			  
		}
		
		creditoDAO.deleteByCriteria( " NUFIN = " + nuFin );
		
	}
	
	
	
	
	
	private static void openSession() throws SQLException {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbc = dwfEntityFacade.getJdbcWrapper();
		jdbc.openSession();
		nativeSql = new NativeSql(jdbc);
	}

	private static void closeSession() {
		JdbcWrapper.closeSession(jdbc);
		NativeSql.releaseResources(nativeSql);
	}
	
	
	@Override
	public void afterUpdate(PersistenceEvent event) throws Exception {
		gerarCredito(event);	
	}
	
	@Override
	public void beforeDelete(PersistenceEvent event) throws Exception {
	}

	@Override
	public void beforeCommit(TransactionContext tranCtx) throws Exception {
	}

	@Override
	public void beforeInsert(PersistenceEvent event) throws Exception {
	}

	@Override
	public void beforeUpdate(PersistenceEvent event) throws Exception {
	}

	@Override
	public void afterInsert(PersistenceEvent event) throws Exception {
		gerarCredito(event);
	}

	@Override
	public void afterDelete(PersistenceEvent event) throws Exception {
	}
	
	

	
	
}

