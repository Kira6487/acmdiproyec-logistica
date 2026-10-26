(() => {
  const documentElement = document.documentElement;
  const toggle = document.querySelector('[data-nav-toggle]');
  const navigation = document.querySelector('[data-navigation]');
  const mobileViewport = window.matchMedia('(max-width: 47.99rem)');

  documentElement.classList.add('js');

  if (!toggle || !navigation) {
    return;
  }

  const setNavigationOpen = (open) => {
    toggle.setAttribute('aria-expanded', String(open));
    navigation.dataset.collapsed = String(!open);
  };

  const adaptNavigation = (event) => {
    setNavigationOpen(!event.matches);
  };

  adaptNavigation(mobileViewport);
  toggle.addEventListener('click', () => {
    const isOpen = toggle.getAttribute('aria-expanded') === 'true';
    setNavigationOpen(!isOpen);
  });
  mobileViewport.addEventListener('change', adaptNavigation);
})();
