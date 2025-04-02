package br.com.sankhya.truss.faturapedidobotica;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.mgecomercial.model.facades.helpper.FaturamentoHelper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.CentralFaturamento;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.SPBeanUtils;
import br.com.sankhya.ws.ServiceContext;

public class FaturaPedidoBotica implements ScheduledAction {

	@Override
	public void onTime(ScheduledActionContext ctx) {
		// TODO Auto-generated method stub
		
		
		JdbcWrapper jdbc = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		JapeWrapper cabDAO = JapeFactory.dao("CabecalhoNota");
		
		
		jdbc = dwfEntityFacade.getJdbcWrapper();
		try {
			jdbc.openSession();
			
			NativeSql q = new NativeSql(jdbc);
			ResultSet r = null;
			
			r = q.executeQuery("SELECT NUNOTA FROM TGFCAB WHERE NVL(AD_FATURABOTICA,'N') = 'S' AND NVL(AD_FATURADOBOTICA,'N') = 'N'");
			
			while(r.next()) {
				faturamentoAutomatico(r.getBigDecimal("NUNOTA"), "1");
				cabDAO.prepareToUpdateByPK(r.getBigDecimal("NUNOTA"))
				.set("AD_FATURADOBOTICA", "S")
				.update();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void faturamentoAutomatico(BigDecimal nuNota, String serie) {
		
		JapeSession.SessionHandle hnd = null;
	    ServiceContext sctx = null;
	    
	    try {
	    	hnd = JapeSession.open();	    	
	    	AuthenticationInfo auth = new AuthenticationInfo("SUP", BigDecimal.ZERO, BigDecimal.ZERO, Integer.valueOf(0));
	        auth.makeCurrent();
	    	sctx = new ServiceContext(null);
	    	sctx.setAutentication(auth);
	    	sctx.makeCurrent();
	    	SPBeanUtils.setupContext(sctx);
	    	JapeSessionContext.putProperty("agendador.faturamento.pedido", Boolean.valueOf(true));	    	
	    } catch (Exception e) {
			e.printStackTrace();
		} finally {
	    	JapeSession.close(hnd);
	    } 
	    
	    hnd = null;
	    try {
	    	
	    	hnd = JapeSession.open();
	        hnd.setPriorityLevel(JapeSession.LOW_PRIORITY);
	        
	        CentralFaturamento.ConfiguracaoFaturamento cfg = new CentralFaturamento.ConfiguracaoFaturamento();
	        
	        cfg.setUsaTopDestino(true);
	        cfg.setDtFaturamento(new Timestamp(System.currentTimeMillis()));
	        cfg.setUmaNotaPorPedido(true);
	        cfg.setSerie(serie);
	        cfg.setFaturamentoNormal(true);
	        cfg.setConfirmarNota(false);
	        cfg.setFaturaEmEstoque(false);
	        cfg.setDeixarItemPendente(false);
	        cfg.setValidarData(false);
	        cfg.setEhWizardFaturamento(false);
	        cfg.setNfeDevolucaoViaRecusa(false);
	        Collection<BigDecimal> notas = new ArrayList<>();
	        notas.add(nuNota);
	        
	        FaturamentoHelper.faturarInterno(sctx, hnd, cfg, notas, null);
	        
	    } catch (Exception e) {

			e.printStackTrace();
			
	    } finally {
	    	
	        JapeSession.close(hnd);
	        hnd = null;
	        
	    } 
	}

}
