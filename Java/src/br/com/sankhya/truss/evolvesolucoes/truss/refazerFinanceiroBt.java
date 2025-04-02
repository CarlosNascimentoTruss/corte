/*     */ package br.com.evolvesolucoes.truss;
/*     */ 
/*     */ import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
/*     */ import br.com.sankhya.extensions.actionbutton.ContextoAcao;
/*     */ import br.com.sankhya.extensions.actionbutton.Registro;
/*     */ import br.com.sankhya.jape.EntityFacade;
/*     */ import br.com.sankhya.jape.bmp.PersistentLocalEntity;
/*     */ import br.com.sankhya.jape.core.JapeSession;
/*     */ import br.com.sankhya.jape.dao.JdbcWrapper;
/*     */ import br.com.sankhya.jape.sql.NativeSql;
/*     */ import br.com.sankhya.jape.util.FinderWrapper;
/*     */ import br.com.sankhya.jape.util.JapeSessionContext;
/*     */ import br.com.sankhya.jape.vo.DynamicVO;
/*     */ import br.com.sankhya.jape.vo.EntityVO;
/*     */ import br.com.sankhya.modelcore.auth.AuthenticationInfo;
/*     */ import br.com.sankhya.modelcore.comercial.CentralItemNota;
/*     */ import br.com.sankhya.modelcore.comercial.impostos.ImpostosHelpper;
/*     */ import br.com.sankhya.modelcore.dwfdata.vo.ItemNotaVO;
/*     */ import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*     */ import br.com.sankhya.modelcore.util.LockTableUtils;
/*     */ import br.com.sankhya.modelcore.util.MGECoreParameter;
/*     */ import com.sankhya.util.BigDecimalUtil;
/*     */ import com.sankhya.util.FinalWrapper;
/*     */ import com.sankhya.util.StringUtils;
/*     */ import java.math.BigDecimal;
/*     */ import java.sql.ResultSet;
/*     */ import java.util.Collection;
/*     */ import java.util.Iterator;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class refazerFinanceiroBt
/*     */   implements AcaoRotinaJava
/*     */ {
/*     */   public void doAction(ContextoAcao contexto) throws Exception {
/*  46 */     JdbcWrapper jdbc = null;
/*  47 */     JapeSession.SessionHandle hdn = null;
/*  48 */     final EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
/*     */ 
/*     */     
/*  51 */     BigDecimal nuNota = BigDecimal.ZERO;
/*     */     
/*  53 */     Registro[] cab = contexto.getLinhas(); byte b; int i;
/*     */     Registro[] arrayOfRegistro1;
/*  55 */     for (i = (arrayOfRegistro1 = cab).length, b = 0; b < i; ) { Registro registro = arrayOfRegistro1[b];
/*  56 */       nuNota = (BigDecimal)registro.getCampo("NUNOTA");
/*     */       
/*     */       try {
/*  59 */         hdn = JapeSession.getCurrentSession().getTopMostHandle();
/*  60 */         jdbc = dwfEntityFacade.getJdbcWrapper();
/*  61 */         FinalWrapper<String> chaveMailRefazFin = new FinalWrapper();
/*     */         
/*  63 */         setupContext();
/*     */         
/*  65 */         NativeSql queNotas = new NativeSql(jdbc);
/*     */         
/*  67 */         queNotas.appendSql(
/*  68 */             "SELECT CAB.NUNOTA FROM TGFCAB CAB WHERE  CAB.NUNOTA = " + 
/*  69 */             nuNota);
/*     */         
/*  71 */         final ResultSet rsFazerfinJob = queNotas.executeQuery();
/*     */ 
/*     */ 
/*     */         
/*  75 */         if (rsFazerfinJob.next()) {
/*     */           do {
/*  77 */             hdn.execWithTX(new JapeSession.TXBlock()
/*     */                 {
/*     */                   
/*     */                   public void doWithTx() throws Exception
/*     */                   {
/*  82 */                     BigDecimal nuNota = rsFazerfinJob.getBigDecimal("NUNOTA");
/*  83 */                     LockTableUtils.lockNotaBD(nuNota);
/*     */                     
/*  85 */                     PersistentLocalEntity cabEntity = dwfEntityFacade.findEntityByPrimaryKey("CabecalhoNota", new Object[] { nuNota });
/*  86 */                     DynamicVO cabVO = (DynamicVO)cabEntity.getValueObject();
/*     */ 
/*     */ 
/*     */                     
/*  90 */                     Collection<?> itens = dwfEntityFacade.findByDynamicFinder(
/*  91 */                         new FinderWrapper("ItemNota", "this.NUNOTA = ?", new Object[] { nuNota }));
/*  92 */                     CentralItemNota centralItemNota = new CentralItemNota();
/*     */                     
/*  94 */                     for (Iterator<?> ite = itens.iterator(); ite.hasNext(); ) {
/*  95 */                       PersistentLocalEntity itemEntity = (PersistentLocalEntity)ite.next();
/*  96 */                       ItemNotaVO itemVO = (ItemNotaVO)((DynamicVO)itemEntity.getValueObject())
/*  97 */                         .wrapInterface(ItemNotaVO.class);
/*     */ 
/*     */                       
/*     */                       try {
/* 101 */                         centralItemNota.recalcularValores("QTDNEG", 
/* 102 */                             itemVO.asBigDecimalOrZero("QTDNEG").toString(), (DynamicVO)itemVO, nuNota);
/*     */                         
/* 104 */                         itemEntity.setValueObject((EntityVO)itemVO);
/*     */                       }
/* 106 */                       catch (Exception e) {
/*     */ 
/*     */ 
/*     */                         
/* 110 */                         System.out.println("Erro de calcular imposto " + e);
/*     */                       } 
/*     */                     } 
/*     */ 
/*     */                     
/* 115 */                     ImpostosHelpper impostoHelper = new ImpostosHelpper();
/*     */                     
/* 117 */                     impostoHelper.carregarNota(nuNota);
/* 118 */                     impostoHelper.calculaICMS(true);
/* 119 */                     impostoHelper.totalizarNota(nuNota);
/* 120 */                     impostoHelper.salvarNota();
/*     */                   }
/*     */                 });
/*     */ 
/*     */             
/* 125 */             Thread.sleep(100L);
/* 126 */           } while (rsFazerfinJob.next());
/*     */         }
/*     */         
/* 129 */         rsFazerfinJob.close();
/*     */       }
/* 131 */       catch (Exception e) {
/* 132 */         e.printStackTrace();
/*     */       } finally {
/*     */         
/* 135 */         JdbcWrapper.closeSession(jdbc);
/*     */       } 
/*     */       b++; }
/*     */   
/*     */   }
/*     */   private String getMensagemEmail(EntityFacade dwfFacade, BigDecimal nota) throws Exception {
/* 141 */     StringBuffer messageBuf = new StringBuffer();
/*     */     
/* 143 */     messageBuf.append("<html><body style=\"font-family: verdana; font-size:12px;\">");
/* 144 */     messageBuf.append("<b>Atenção: Nota não processada:</b><br/><br/>");
/*     */     
/* 146 */     messageBuf.append(
/* 147 */         "<table border=\"1\" border-style=\"solid\" style=\"width: 100%; font-family: verdana; font-size:12px; border-collapse: collapse\">");
/* 148 */     messageBuf.append("<tr>");
/* 149 */     messageBuf.append("\t<td align=\"right\"><b>").append("Nro. Nota").append("</b></td>");
/* 150 */     messageBuf.append("\t<td align=\"left\"><b>").append("Empresa").append("</b></td>");
/* 151 */     messageBuf.append("\t<td align=\"right\"><b>").append("Valor Nota").append("</b></td>");
/* 152 */     messageBuf.append("</tr>");
/*     */     
/* 154 */     DynamicVO notaVO = (DynamicVO)dwfFacade.findEntityByPrimaryKeyAsVO("CabecalhoNota", nota);
/*     */     
/* 156 */     messageBuf.append("<tr>");
/* 157 */     messageBuf.append("\t<td align=\"right\">").append(nota.toString()).append("</td>");
/* 158 */     messageBuf.append("\t<td align=\"left\">")
/* 159 */       .append(StringUtils.getNullAsEmpty(notaVO.asString("Empresa.RAZAOSOCIAL"))).append("</td>");
/* 160 */     messageBuf.append("\t<td align=\"right\">R$")
/* 161 */       .append(StringUtils.formatNumeric("###,##0.00", notaVO.asBigDecimal("VLRNOTA"))).append("</td>");
/* 162 */     messageBuf.append("</tr>");
/*     */     
/* 164 */     messageBuf.append("</table><br/>");
/* 165 */     messageBuf.append("Favor verificar no sistema os motivos do não processamento do documento.<br>");
/* 166 */     messageBuf.append(
/* 167 */         "O documento apresentado nesse e-mail não foi processado na rotina do job de refazer financeiro.<br><br>");
/* 168 */     messageBuf.append("<b>Mensagem gerada automaticamente. Não responder.</b><br><br>");
/* 169 */     messageBuf.append("</body></html>");
/*     */     
/* 171 */     insereLogoSankhyaSePermitido(messageBuf);
/*     */     
/* 173 */     return messageBuf.toString();
/*     */   }
/*     */ 
/*     */   
/*     */   private void criarEmailNaFila(EntityFacade dwfFacade, String destinatario, String assunto, String mensagem, BigDecimal codSMTP) throws Exception {
/* 178 */     DynamicVO filaVO = (DynamicVO)dwfFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
/*     */     
/* 180 */     filaVO.setProperty("EMAIL", destinatario);
/* 181 */     filaVO.setProperty("CODCON", BigDecimal.ZERO);
/* 182 */     filaVO.setProperty("CODMSG", null);
/* 183 */     filaVO.setProperty("STATUS", "Pendente");
/* 184 */     filaVO.setProperty("TIPOENVIO", "E");
/* 185 */     filaVO.setProperty("MAXTENTENVIO", BigDecimalUtil.valueOf(3L));
/* 186 */     filaVO.setProperty("ASSUNTO", assunto);
/* 187 */     filaVO.setProperty("MIMETYPE", "text/html");
/* 188 */     filaVO.setProperty("MENSAGEM", mensagem.toCharArray());
/* 189 */     filaVO.setProperty("TIPODOC", "E");
/* 190 */     filaVO.setProperty("CODSMTP", codSMTP);
/*     */     
/* 192 */     dwfFacade.createEntity("MSDFilaMensagem", (EntityVO)filaVO);
/*     */   }
/*     */   
/*     */   private void setupContext() {
/* 196 */     AuthenticationInfo auth = AuthenticationInfo.getCurrent();
/* 197 */     JapeSessionContext.putProperty("usuario_logado", auth.getUserID());
/* 198 */     JapeSessionContext.putProperty("authInfo", auth);
/* 199 */     JapeSessionContext.putProperty("br.com.sankhya.com.CentralCompraVenda", Boolean.TRUE);
/* 200 */     JapeSessionContext.putProperty("br.com.sankhya.com.CentralCompraVenda", Boolean.TRUE);
/* 201 */     JapeSession.putProperty("ItemNota.incluindo.alterando.pela.central", Boolean.TRUE);
/*     */   }
/*     */   
/*     */   private void insereLogoSankhyaSePermitido(StringBuffer messageBuf) {
/* 205 */     boolean removerLogo = false;
/*     */     
/* 207 */     String strParamRemoverLogo = (String)(new MGECoreParameter()).getParameter("REMOVLOGOSNKNFE", 4, 0);
/* 208 */     if (strParamRemoverLogo != null && "S".equals(strParamRemoverLogo)) {
/* 209 */       removerLogo = true;
/*     */     }
/* 211 */     if (!removerLogo) {
/* 212 */       messageBuf.append("<HR WIDTH=100% style=\"border:1px solid #228B22;\">");
/* 213 */       messageBuf.append("<img src= \"http://aplicacoes.sankhya.com.br/imagens/logo_sankhya.jpg\"><br>");
/* 214 */       messageBuf.append(
/* 215 */           "<i>Powered by Sankhya Gestão de Negócios. -</i> <a href=\"http://www.sankhya.com.br\">www.sankhya.com.br</a><br>");
/* 216 */       messageBuf.append("<HR WIDTH=100% style=\"border:1px solid #228B22;\">");
/*     */     } 
/*     */   }
/*     */   
/*     */   private NativeSql getSqlBuscaParam(boolean ehTexto, JdbcWrapper jdbcWrapper) {
/* 221 */     NativeSql sqlBuscaParam = new NativeSql(jdbcWrapper);
/* 222 */     sqlBuscaParam.appendSql("\tSELECT ");
/* 223 */     if (ehTexto) {
/* 224 */       sqlBuscaParam.appendSql("\t\tINTEIRO ");
/*     */     } else {
/* 226 */       sqlBuscaParam.appendSql("\t\tTEXTO ");
/*     */     } 
/* 228 */     sqlBuscaParam.appendSql("\tFROM ");
/* 229 */     sqlBuscaParam.appendSql("\t\tTSIPAR PAR ");
/* 230 */     sqlBuscaParam.appendSql("\tWHERE ");
/* 231 */     sqlBuscaParam.appendSql("\t \tPAR.CHAVE = :CHAVE");
/*     */     
/* 233 */     return sqlBuscaParam;
/*     */   }
/*     */ }


/* Location:              C:\Users\daniel.ratkevicius_s\Desktop\gerarroyalties.jar!\br\com\evolvesolucoes\truss\refazerFinanceiroBt.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */