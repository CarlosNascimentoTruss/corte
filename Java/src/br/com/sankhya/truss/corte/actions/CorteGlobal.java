package br.com.sankhya.truss.corte.actions;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import br.com.sankhya.truss.corte.depara.actions.DePara;
import br.com.sankhya.truss.corte.depara.actions.SubstituicaoExec;
import org.jdom.Element;

import com.sankhya.util.TimeUtils;
import com.sankhya.util.XMLUtils;

import br.com.sankhya.dwf.services.ServiceUtils;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.mgecomercial.model.centrais.cac.CACSP;
import br.com.sankhya.mgecomercial.model.centrais.cac.CACSPHome;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.CentralItemNota;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.truss.corte.helper.CorteHelper;
import br.com.sankhya.truss.corte.scheduled.CorteLocal;
import br.com.sankhya.ws.ServiceContext;

public class CorteGlobal implements AcaoRotinaJava {
	private static SessionHandle hnd = null;
	private static JdbcWrapper jdbc	= null;
	private static Boolean isOpen = Boolean.FALSE;

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		BigDecimal codUsuLogado = ctx.getUsuarioLogado();
		JapeWrapper cabDAO = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA);
		JapeWrapper parDAO = JapeFactory.dao(DynamicEntityNames.PARCEIRO);



		try {

			Registro[] linhas = ctx.getLinhas();

			JapeSessionContext.putProperty("br.com.sankhya.com.CentralCompraVenda", Boolean.TRUE);
			JapeSessionContext.putProperty("ItemNota.incluindo.alterando.pela.central", Boolean.TRUE);


			for(Registro linha : linhas) {
				BigDecimal nunota = (BigDecimal) linha.getCampo("NUNOTA");

				DePara dePara = new DePara();
				dePara.acao(nunota,codUsuLogado);

				DynamicVO cabVO = cabDAO.findByPK(nunota);
				DynamicVO parVO = parDAO.findByPK(cabVO.asBigDecimal("CODPARC"));

				String localSeparacao = parVO.asString("AD_LOCALSEPARACAO") == null ? "1" : parVO.asString("AD_LOCALSEPARACAO");

				if ("1".equals(localSeparacao)) {
					CorteExpedicaoTruss.executaCorte(nunota);
				} else {
					CorteExpedicaoOperador.executaCorte(nunota);
				}
			}

			ctx.setMensagemRetorno("Corte Executado com Sucesso.");
		} catch(Exception e) {
			e.printStackTrace();
			throw new Exception("Erro ao executar a a" + e.getMessage());
		}
	}



}
