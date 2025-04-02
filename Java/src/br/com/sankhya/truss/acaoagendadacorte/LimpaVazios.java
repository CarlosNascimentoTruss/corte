package br.com.sankhya.truss.acaoagendadacorte;

import java.sql.ResultSet;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class LimpaVazios implements ScheduledAction {

	@Override
	public void onTime(ScheduledActionContext ctx) {
		// TODO Auto-generated method stub
		
		JdbcWrapper jdbc = null;
		
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbc = dwfEntityFacade.getJdbcWrapper();
			jdbc.openSession();
			
			JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
			
			
			NativeSql q = new NativeSql(jdbc);
			ResultSet r = q.executeQuery(" select CAB.AD_CORTOU AS NUNOTA_ORIGINAL, CAB.NUNOTA AS NUNOTA_CRIADA , CAB.DTNEG, ITE.CODPROD, ITE.QTDNEG, ITE.CODUSU "
					+ " from tgfcab  CAB "
					+ " LEFT JOIN TGFITE ITE ON ITE.NUNOTA = CAB.NUNOTA "
					+ " where ad_cortou is not null "
					+ " and ad_cortou not in ('S','N') "
					+ " and not exists (SELECT 1 FROM TGFITE I WHERE I.NUNOTA = CAB.NUNOTA ) "
					+ " and dtneg >= '01/01/2025' "
					+ " ORDER BY CAB.DTNEG DESC ");
			
			while(r.next()) {
				cabDAO.deleteByCriteria("NUNOTA = ?", r.getBigDecimal("NUNOTA_CRIADA"));
			}
			
			
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			JdbcWrapper.closeSession(jdbc);
		}
		
		
		
		
		
	}

}
