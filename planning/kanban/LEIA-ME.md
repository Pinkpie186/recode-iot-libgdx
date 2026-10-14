# ReCode — importar cartões para Issues

Este pacote contém os **23 cartões** solicitados, com área, prioridade e checklist, em `cartoes.json`. Ele foi preparado para `Pinkpie186/recode-iot-libgdx`.

A **Issue #1** já existe no repositório com o primeiro cartão. O importador confere o título dela e **não a altera**. Ao executar, cria apenas os cartões restantes que ainda não existirem. Se a importação parar no meio, basta repetir: títulos já encontrados são ignorados.

## Como importar no Windows

1. Extraia todo o ZIP para uma pasta.
2. Se você já executou `gh auth login`, o importador usa essa sessão. Caso contrário, crie um token GitHub com acesso ao repositório e permissão **Issues: Read and write**. Guarde o token em local privado e não o envie por mensagem.
3. Abra o PowerShell dentro da pasta extraída e execute:

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Importar-Issues.ps1
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Importar-Issues.ps1 -Executar
   ```

A primeira execução só verifica os 23 cartões; a segunda importa as Issues. Ela usa a autenticação do GitHub CLI, se disponível, ou pede o token sem exibi-lo na tela. A opção `-ExecutionPolicy Bypass` vale apenas para o novo processo iniciado por esse comando.

O importador usa a API do GitHub. Ele não coloca o token no ZIP nem modifica arquivos do repositório. A área e a prioridade aparecem no corpo de cada Issue, junto do checklist.
