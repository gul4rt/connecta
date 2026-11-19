// Search bar de bairros: consulta /bairros?termo=... (ViaCEP, São Paulo/SP)
// e mostra sugestões. Sem JavaScript, o campo continua funcionando digitando direto.
(function () {
  var campo = document.getElementById('bairro');
  var lista = document.getElementById('sugestoes-bairros');
  if (!campo || !lista) return;

  var buscaAtual = null;

  function limpar() {
    lista.hidden = true;
    lista.innerHTML = '';
  }

  function escolher(nome) {
    campo.value = nome;
    limpar();
    campo.focus();
  }

  campo.addEventListener('input', function () {
    var termo = campo.value.trim();
    if (termo.length < 3) {
      limpar();
      return;
    }
    if (buscaAtual) buscaAtual.abort();
    buscaAtual = new AbortController();
    fetch('/bairros?termo=' + encodeURIComponent(termo), { signal: buscaAtual.signal })
      .then(function (resposta) { return resposta.json(); })
      .then(function (bairros) {
        lista.innerHTML = '';
        if (!bairros.length) { limpar(); return; }
        bairros.forEach(function (nome) {
          var item = document.createElement('li');
          var botao = document.createElement('button');
          botao.type = 'button';
          botao.textContent = nome;
          botao.addEventListener('click', function () { escolher(nome); });
          item.appendChild(botao);
          lista.appendChild(item);
        });
        lista.hidden = false;
      })
      .catch(function () { limpar(); }); // requisição cancelada ou ViaCEP fora do ar
  });

  document.addEventListener('click', function (evento) {
    if (!lista.contains(evento.target) && evento.target !== campo) limpar();
  });
})();
