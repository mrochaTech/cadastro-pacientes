---
version: alpha
colors:
  background: "#F2F6F4"
  surface: "#FFFFFF"
  sidebar: "#153D3A"
  sidebarMuted: "#9BC8BE"
  sidebarText: "#D6E7E2"
  sidebarSelected: "#24544F"
  primary: "#08766B"
  primarySoft: "#E3F1EE"
  tableHeader: "#F7F9F8"
  text: "#1D302D"
  muted: "#687A76"
  border: "#DCE6E2"
  error: "#A43E3E"
  warning: "#8A5A17"
typography:
  sans:
    fontFamily: "Segoe UI, Arial, sans-serif"
    fontSize: "14px"
    lineHeight: "1.4"
  title:
    fontFamily: "Segoe UI, Arial, sans-serif"
    fontSize: "27px"
    lineHeight: "1.2"
spacing:
  control: "8px"
  card: "20px"
  workspace: "34px"
omitted:
  - section: rounded
    reason: "A interface usa componentes nativos do Swing com contornos retos."
  - section: components
    reason: "Os estilos são aplicados diretamente aos componentes Swing em CadastroPacientesApp.java."
---

## Overview

Interface de cadastro local para uma recepção pequena. O produto é uma ferramenta de trabalho em desktop: leitura rápida, campos claros e navegação previsível têm prioridade sobre decoração. A assinatura visual é a faixa verde escura de navegação, que mantém as duas telas no mesmo contexto.

O público e o conteúdo são brasileiros e em português. O app trata dados pessoais básicos; a linguagem deve ser simples, respeitosa e direta.

## Colors

O fundo verde-acinzentado separa a área de trabalho dos cartões brancos. Verde profundo identifica a navegação e o verde médio marca a ação principal. Texto, borda, erro e aviso usam papéis próprios; não use apenas cor para comunicar validação.

As cores do front matter correspondem às constantes de cor no código `CadastroPacientesApp.java`; os nomes preservam os papéis indicados aqui.

## Typography

Use Segoe UI como primeira escolha, com Arial e sans-serif como fallback. O título de tela tem 27 px; rótulos e ações usam peso forte; texto auxiliar usa 11–12 px e cor muted. Mantenha os rótulos em português e evite abreviações em campos.

## Layout

Desktop-first, com faixa lateral fixa de navegação e conteúdo em uma coluna ampla. A tela de cadastro agrupa nome, idade e contato em um único cartão; a lista mantém pesquisa, tabela e paginação juntas. A janela tem tamanho mínimo que preserva a leitura dos campos e das colunas.

## Elevation & Depth

Prefira cartões planos, fundo sutil e uma borda fina. Não use sombras fortes nem gradientes; contraste de superfície e espaçamento definem a hierarquia.

## Shapes

Contornos retos e discretos remetem a uma ficha organizada. Botões e campos preservam áreas de foco visíveis e alvos confortáveis.

## Components

- Ação primária: fundo verde, texto branco, usada para salvar ou iniciar o primeiro cadastro.
- Ações secundárias: fundo branco, contorno cinza-esverdeado.
- Navegação selecionada: verde um pouco mais claro que a faixa lateral.
- Erros de formulário: texto junto ao formulário; falhas de arquivo: mensagem persistente na barra inferior.
- Tabela: cabeçalho claro, linhas com altura confortável e navegação de 25 registros por página.

## Do's and Don'ts

- Mantenha os mesmos termos nas ações e nos retornos de estado.
- Preserve o texto digitado quando uma validação ou gravação falhar.
- Não inclua informações médicas, imagens decorativas ou indicadores clínicos sem requisito explícito.
- Não apresente o armazenamento local como criptografado ou protegido por autenticação.
