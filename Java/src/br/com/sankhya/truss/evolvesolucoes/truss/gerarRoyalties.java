/*     */ package br.com.evolvesolucoes.truss;
/*     */ import java.math.BigDecimal;
/*     */ import java.math.RoundingMode;
/*     */ import java.sql.ResultSet;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collection;

/*     */ import com.sankhya.util.BigDecimalUtil;

/*     */ 
/*     */ import br.com.sankhya.commons.xml.Element;
/*     */ import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
/*     */ import br.com.sankhya.extensions.actionbutton.ContextoAcao;
/*     */ import br.com.sankhya.extensions.actionbutton.Registro;
/*     */ import br.com.sankhya.jape.EntityFacade;
/*     */ import br.com.sankhya.jape.core.JapeSession;
/*     */ import br.com.sankhya.jape.dao.JdbcWrapper;
/*     */ import br.com.sankhya.jape.sql.NativeSql;
/*     */ import br.com.sankhya.jape.util.JapeSessionContext;
/*     */ import br.com.sankhya.jape.vo.DynamicVO;
/*     */ import br.com.sankhya.jape.vo.PrePersistEntityState;
/*     */ import br.com.sankhya.modelcore.auth.AuthenticationInfo;
/*     */ import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
/*     */ import br.com.sankhya.modelcore.util.EntityFacadeFactory;
/*     */ import br.com.sankhya.modelcore.util.SPBeanUtils;
/*     */ import br.com.sankhya.ws.ServiceContext;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class gerarRoyalties
/*     */   implements AcaoRotinaJava
/*     */ {
/*     */   public void doAction(ContextoAcao contexto) throws Exception {
/*  35 */     JapeSession.SessionHandle hnd = null;
/*  36 */     BigDecimal nunotaRoyalties = new BigDecimal(0);
/*  37 */     BigDecimal nunotaTaxa = new BigDecimal(0);
/*  38 */     JdbcWrapper jdbc = null;
/*     */     
/*  40 */     Registro[] lancamentos = contexto.getLinhas();
/*     */     
/*  42 */     Boolean processarRoyalties = Boolean.TRUE;
/*     */     
/*  44 */     processarRoyalties = Boolean.valueOf(contexto.confirmarSimNao("Royalties", 
/*  45 */           "Processamento de Royalties e Taxa de publicidade<br> Deseja continuar?", 1));
/*     */     
/*  47 */     if (processarRoyalties.booleanValue()) {
/*     */       byte b; int i; Registro[] arrayOfRegistro;
/*  49 */       for (i = (arrayOfRegistro = lancamentos).length, b = 0; b < i; ) { Registro registro = arrayOfRegistro[b];
/*     */         
/*     */         try {
/*  52 */           hnd = JapeSession.open();
/*  53 */           EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
/*  54 */           CACHelper cacHelper = new CACHelper();
/*  55 */           ServiceContext ctx = new ServiceContext(null);
/*     */           
/*  57 */           AuthenticationInfo authInfo = new AuthenticationInfo("SUP ", BigDecimalUtil.ZERO_VALUE, 
/*  58 */               BigDecimalUtil.ZERO_VALUE, Integer.valueOf(0));
/*  59 */           authInfo.makeCurrent();
/*  60 */           Element requestBody = new Element("requestBody");
/*  61 */           ctx.setRequestBody((Element)requestBody);
/*  62 */           ctx.setAutentication(authInfo);
/*  63 */           ctx.makeCurrent();
/*     */           
/*  65 */           jdbc = dwfEntityFacade.getJdbcWrapper();
/*  66 */           jdbc.openSession();
/*     */           
/*  68 */           SPBeanUtils.setupContext(ctx);
/*     */           
/*  70 */           BigDecimal codUsu = (BigDecimal)JapeSessionContext.getProperty("usuario_logado");
/*     */           
/*  72 */           if ((contexto.getLinhas()).length == 0) {
/*  73 */             throw new Exception("Selecione um registro");
/*     */           }
/*  75 */           if ((contexto.getLinhas()).length > 1) {
/*  76 */             throw new Exception("Selecione apenas um registro");
/*     */           }
/*     */           
/*  79 */           DynamicVO empresaFinVO = (DynamicVO)dwfEntityFacade
/*  80 */             .findEntityByPrimaryKeyAsVO("EmpresaFinanceiro", registro.getCampo("CODEMP"));
/*     */           
/*  82 */           if (String.valueOf(1).equals(registro.getCampo("AD_STATUSROYAL"))) {
/*  83 */             contexto.mostraErro("Royaltes ja gerados, não é possivel continuar");
/*     */           }
/*     */ 
/*     */           
/*  87 */           if (empresaFinVO.getProperty("AD_CODSERV") == null) {
/*  88 */             contexto.mostraErro("Cadastrar para empresa " + empresaFinVO.getProperty("CODEMP") + 
/*  89 */                 " o campo Código de serviço, dentro do menu Comercial >> Preferencias >> Empresa Aba Roaylties / Taxas");
/*     */           }
/*     */           
/*  92 */           if (empresaFinVO.getProperty("AD_CODEMPSERV") == null) {
/*  93 */             contexto.mostraErro("Cadastrar para empresa " + empresaFinVO.getProperty("CODEMP") + 
/*  94 */                 " o campo Empresa Serviço, dentro do menu Comercial >> Preferencias >> Empresa Aba Roaylties / Taxas");
/*     */           }
/*     */           
/*  97 */           if (empresaFinVO.getProperty("AD_PERCROYALTIES") == null) {
/*  98 */             contexto.mostraErro("Cadastrar para empresa " + empresaFinVO.getProperty("CODEMP") + 
/*  99 */                 " o % de royalties e / ou  % Taxa Publicidade, dentro do menu Comercial >> Preferencias >> Empresa Aba Roaylties / Taxas");
/*     */           }
/*     */           
/* 102 */           if (empresaFinVO.getProperty("AD_PERCTAXA") == null) {
/* 103 */             contexto.mostraErro("Cadastrar para empresa " + empresaFinVO.getProperty("CODEMP") + 
/* 104 */                 " o % de royalties e / ou  % Taxa Publicidade, dentro do menu Comercial >> Preferencias >> Empresa Aba Roaylties / Taxas");
/*     */           }
/*     */           
/* 107 */           if (empresaFinVO.getProperty("AD_CODTIPOPER") == null) {
/* 108 */             contexto.mostraErro("Cadastrar para empresa " + empresaFinVO.getProperty("CODEMP") + 
/* 109 */                 " o campo Cód.Tipo Operação, dentro do menu Comercial >> Preferencias >> Empresa Aba Roaylties / Taxas");
/*     */           }
/*     */ 
/*     */           
/* 113 */           NativeSql queNotas = new NativeSql(jdbc);
/*     */           
/* 115 */           queNotas.appendSql("select sum(ite.vlrtot) vlrtot   from tgfite ite where ite.usoprod !='D' and ite.nunota= " + 
/* 116 */               registro.getCampo("NUNOTA"));
/*     */           
/* 118 */           ResultSet rsPerc = queNotas.executeQuery();
/*     */           
/* 120 */           if (rsPerc.next())
/*     */           {
/* 122 */             BigDecimal codCenCus = (BigDecimal)empresaFinVO.getProperty("AD_CODCENCUSROY");
/* 123 */             BigDecimal codNat = (BigDecimal)empresaFinVO.getProperty("AD_CODNATROY");
/* 124 */             BigDecimal codParc = (BigDecimal)registro.getCampo("CODPARC");
/* 125 */             BigDecimal codTipoPer = (BigDecimal)empresaFinVO.getProperty("AD_CODTIPOPER");
/* 126 */             BigDecimal codtipvenda = BigDecimal.ONE;
/* 127 */             BigDecimal codEmp = (BigDecimal)empresaFinVO.getProperty("AD_CODEMPSERV");
/*     */             
/* 129 */             nunotaRoyalties = criaCabecalho(contexto, codEmp, codEmp, codCenCus, codNat, codParc, 
/* 130 */                 codTipoPer, codtipvenda, (BigDecimal)registro.getCampo("NUNOTA"), codUsu);
/*     */ 
/*     */             
/* 133 */             BigDecimal valorNota = rsPerc.getBigDecimal("VLRTOT");
/*     */             
/* 135 */             BigDecimal royalties = (BigDecimal)empresaFinVO.getProperty("AD_PERCROYALTIES");
/* 136 */             BigDecimal percRoyalties = royalties.divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP);
/*     */             
/* 138 */             BigDecimal valorUnitario = valorNota.multiply(percRoyalties);
/*     */             
/* 140 */             DynamicVO itemRoyaltiesVO = (DynamicVO)dwfEntityFacade
/* 141 */               .getDefaultValueObjectInstance("ItemNota");
/* 142 */             itemRoyaltiesVO.setPrimaryKey(null);
/* 143 */             itemRoyaltiesVO.setProperty("NUNOTA", nunotaRoyalties);
/* 144 */             itemRoyaltiesVO.setProperty("SEQUENCIA", BigDecimal.ONE);
/* 145 */             itemRoyaltiesVO.setProperty("CODPROD", empresaFinVO.getProperty("AD_CODSERV"));
/* 146 */             itemRoyaltiesVO.setProperty("CONTROLE", " ");
/* 147 */             itemRoyaltiesVO.setProperty("CODLOCALORIG", empresaFinVO.getProperty("LOCALPAD"));
/* 148 */             itemRoyaltiesVO.setProperty("QTDNEG", BigDecimal.ONE);
/* 149 */             itemRoyaltiesVO.setProperty("QTDENTREGUE", BigDecimal.ZERO);
/* 150 */             itemRoyaltiesVO.setProperty("VLRUNIT", valorUnitario);
/* 151 */             itemRoyaltiesVO.setProperty("VLRDESC", BigDecimal.ZERO);
/* 152 */             itemRoyaltiesVO.setProperty("VLRTOT", valorUnitario);
/*     */             
/* 154 */             Collection<PrePersistEntityState> itensNota = new ArrayList<>();
/* 155 */             PrePersistEntityState itemMontado = PrePersistEntityState.build(dwfEntityFacade, 
/* 156 */                 "ItemNota", itemRoyaltiesVO);
/* 157 */             itensNota.add(itemMontado);
/* 158 */             cacHelper.incluirAlterarItem(new BigDecimal(nunotaRoyalties.toString()), ctx, itensNota, true);
/*     */             
/* 160 */             BigDecimal codCenCusTx = (BigDecimal)empresaFinVO.getProperty("AD_CODCENCUSTX");
/* 161 */             BigDecimal codNatTx = (BigDecimal)empresaFinVO.getProperty("AD_CODNATTX");
/* 162 */             BigDecimal codEmpTx = (BigDecimal)empresaFinVO.getProperty("AD_CODEMPTX");
/*     */             
/* 164 */             nunotaTaxa = criaCabecalho(contexto, codEmpTx, codEmpTx, codCenCusTx, codNatTx, codParc, 
/* 165 */                 codTipoPer, codtipvenda, (BigDecimal)registro.getCampo("NUNOTA"), codUsu);
/*     */             
/* 167 */             BigDecimal taxa = (BigDecimal)empresaFinVO.getProperty("AD_PERCTAXA");
/* 168 */             BigDecimal percTaxa = taxa.divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP);
/*     */             
/* 170 */             BigDecimal valorUnitarioTaxa = valorNota.multiply(percTaxa);
/*     */             
/* 172 */             DynamicVO itemTaxaVO = (DynamicVO)dwfEntityFacade
/* 173 */               .getDefaultValueObjectInstance("ItemNota");
/* 174 */             itemTaxaVO.setPrimaryKey(null);
/* 175 */             itemTaxaVO.setProperty("NUNOTA", nunotaTaxa);
/* 176 */             itemTaxaVO.setProperty("SEQUENCIA", BigDecimal.ONE);
/* 177 */             itemTaxaVO.setProperty("CODPROD", empresaFinVO.getProperty("AD_CODTAXA"));
/* 178 */             itemTaxaVO.setProperty("CONTROLE", " ");
/* 179 */             itemTaxaVO.setProperty("CODLOCALORIG", BigDecimal.ZERO);
/* 180 */             itemTaxaVO.setProperty("QTDNEG", BigDecimal.ONE);
/* 181 */             itemTaxaVO.setProperty("QTDENTREGUE", BigDecimal.ZERO);
/* 182 */             itemTaxaVO.setProperty("VLRUNIT", valorUnitarioTaxa);
/* 183 */             itemTaxaVO.setProperty("VLRDESC", BigDecimal.ZERO);
/* 184 */             itemTaxaVO.setProperty("VLRTOT", valorUnitarioTaxa);
/*     */             
/* 186 */             Collection<PrePersistEntityState> itensNotaTaxa = new ArrayList<>();
/* 187 */             PrePersistEntityState itemTaxaMontado = PrePersistEntityState.build(dwfEntityFacade, 
/* 188 */                 "ItemNota", itemTaxaVO);
/* 189 */             itensNotaTaxa.add(itemTaxaMontado);
/* 190 */             cacHelper.incluirAlterarItem(new BigDecimal(nunotaTaxa.toString()), ctx, itensNotaTaxa, true);
/*     */             
/* 192 */             registro.setCampo("AD_STATUSROYAL", String.valueOf(1));
/*     */             
/* 194 */             contexto.setMensagemRetorno(
/* 195 */                 "Pedidos gerados com sucesso " + nunotaRoyalties + " , " + nunotaTaxa);
/*     */           }
/*     */         
/*     */         } finally {
/*     */           
/* 200 */           JapeSession.close(hnd);
/*     */         } 
/*     */         b++; }
/*     */     
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public BigDecimal criaCabecalho(ContextoAcao contexto, BigDecimal codEmp, Object codEmpNegoc, BigDecimal codCenCus, BigDecimal codNat, BigDecimal parceiro, BigDecimal codTipoPer, BigDecimal codtipvenda, BigDecimal nuOrigem, BigDecimal codUsu) throws Exception {
/*     */     try {
/* 212 */       Registro cabecalho = contexto.novaLinha("TGFCAB");
/* 213 */       cabecalho.setCampo("CODEMP", codEmp);
/* 214 */       cabecalho.setCampo("CODEMPNEGOC", codEmpNegoc);
/* 215 */       cabecalho.setCampo("CODCENCUS", codCenCus);
/* 216 */       cabecalho.setCampo("CODNAT", codNat);
/* 217 */       cabecalho.setCampo("CODPARC", parceiro);
/* 218 */       cabecalho.setCampo("CODTIPOPER", codTipoPer);
/* 219 */       cabecalho.setCampo("CODTIPVENDA", codtipvenda);
/* 220 */       cabecalho.setCampo("NUMNOTA", Integer.valueOf(0));
/* 221 */       cabecalho.setCampo("CIF_FOB", String.valueOf("S"));
/* 222 */       cabecalho.setCampo("AD_NUNOTA", nuOrigem);
/* 223 */       cabecalho.setCampo("AD_CODUSUROY", codUsu);
/* 224 */       cabecalho.save();
/*     */       
/* 226 */       return (BigDecimal)cabecalho.getCampo("NUNOTA");
/*     */     }
/* 228 */     catch (Exception e) {
/* 229 */       e.printStackTrace();
/* 230 */       contexto.setMensagemRetorno(e.getMessage());
/*     */       
/* 232 */       return new BigDecimal(0);
/*     */     } 
/*     */   }
/*     */ }


/* Location:              C:\Users\daniel.ratkevicius_s\Desktop\gerarroyalties.jar!\br\com\evolvesolucoes\truss\gerarRoyalties.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */