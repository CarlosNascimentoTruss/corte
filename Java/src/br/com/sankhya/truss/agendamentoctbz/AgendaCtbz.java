package br.com.sankhya.truss.agendamentoctbz;

import org.jdom.Element;


import com.sankhya.util.XMLUtils;

import br.com.sankhya.ctbz.model.facades.AgendamentoCtbzSP;
import br.com.sankhya.ctbz.model.facades.AgendamentoCtbzSPHome;
import br.com.sankhya.dwf.services.ServiceUtils;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.ws.ServiceContext;

import java.math.BigDecimal;

public class AgendaCtbz implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		// TODO Auto-generated method stub
		
		
		AgendamentoCtbzSP agendamentoSP = (AgendamentoCtbzSP) ServiceUtils.getStatelessFacade(AgendamentoCtbzSPHome.JNDI_NAME, AgendamentoCtbzSPHome.class);
		
		ServiceContext ctx = ServiceContext.getCurrent();
		Element requestBody = ctx.getRequestBody();
		Element responseBody = ctx.getBodyElement();

		responseBody.removeContent();
		requestBody.removeContent();
		
		Element criterioElem = new Element("criterio");
		
		XMLUtils.addAttributeElement(criterioElem, "DTFIM", "20/06/2024");
		XMLUtils.addAttributeElement(criterioElem, "DTINICIO", "20/06/2024");
		XMLUtils.addAttributeElement(criterioElem, "DTMOV", "20/06/2024");
		XMLUtils.addAttributeElement(criterioElem, "NROLOTE", BigDecimal.ONE.negate());
		XMLUtils.addAttributeElement(criterioElem, "NUAGENDAMENTO", BigDecimal.valueOf(26));
		
		
		requestBody.addContent(criterioElem);
		
		
		agendamentoSP.processaContabilizacaoManual(ctx);

	}

}
