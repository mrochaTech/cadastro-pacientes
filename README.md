# Cadastro de pacientes

Aplicação desktop em Java Swing para cadastrar e consultar pacientes em duas telas. Os dados ficam em uma coleção durante a execução e são carregados de um arquivo local ao abrir o programa.

## Requisitos

- Java 8 ou superior

## Compilar e executar

Abra um terminal nesta pasta e execute:

```powershell
javac -encoding UTF-8 CadastroPacientesApp.java
java CadastroPacientesApp
```

No primeiro cadastro, o programa cria a pasta `.cadastro-pacientes` dentro da pasta pessoal do usuário e grava `pacientes.tsv`. Cada novo cadastro é salvo antes de aparecer na lista.

## Campos e validações

- Nome completo: obrigatório, de 2 a 100 caracteres e com pelo menos uma letra.
- Idade: obrigatória, número inteiro de 0 a 130.
- Telefone e e-mail: opcionais; quando preenchidos, passam por validação de formato.
- A lista permite pesquisar por nome, telefone ou e-mail e navega em páginas de até 25 registros.

Erros de campo e falhas de leitura/gravação são tratados na interface. Linhas inválidas encontradas no arquivo são ignoradas; antes de substituir um arquivo com linhas inválidas, o programa cria uma cópia `.bak` para preservar o original.

## Dados

O arquivo permanece neste computador e não é criptografado. Use dados fictícios para demonstração; o programa não inclui autenticação, controle de acesso nem recursos de prontuário clínico.
