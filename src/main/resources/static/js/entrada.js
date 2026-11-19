// Melhoria progressiva: o link "Entrar" abre um popup com o formulário de login.
// Sem JavaScript, o link leva para a página /entrar (mesmo formulário, sem popup).
document.querySelectorAll('[data-abrir-entrada]').forEach(function (link) {
  link.addEventListener('click', function (evento) {
    var dialogo = document.getElementById('dialog-entrada');
    if (!dialogo) return;
    evento.preventDefault();
    dialogo.showModal();
    var campo = dialogo.querySelector('input[name="username"]');
    if (campo) campo.focus();
  });
});
document.querySelectorAll('[data-fechar-entrada]').forEach(function (botao) {
  botao.addEventListener('click', function () {
    var dialogo = document.getElementById('dialog-entrada');
    if (dialogo) dialogo.close();
  });
});
