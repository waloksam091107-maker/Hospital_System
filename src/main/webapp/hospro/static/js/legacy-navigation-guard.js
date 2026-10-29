(function (window, document) {
    'use strict';

    function applicationContext() {
        var marker = '/hospro/';
        var index = window.location.pathname.indexOf(marker);
        return index >= 0 ? window.location.pathname.substring(0, index) : '';
    }

    function homeUrl() {
        return applicationContext() + '/hospro/index.html';
    }

    function isPlaceholder(href) {
        return !href || href === 'javascript:void(0)' || href === '#';
    }

    function disablePlaceholderLink(link) {
        link.setAttribute('data-unavailable-link', 'true');
        link.setAttribute('aria-disabled', 'true');
        link.setAttribute('title', '该模板功能暂未开放。');
        link.setAttribute('href', '#');
        link.addEventListener('click', function (event) {
            event.preventDefault();
            window.alert('该功能暂未开放，请使用系统菜单中的已提供功能。');
        });
    }

    function normalizeLegacyTemplateLinks() {
        Array.prototype.forEach.call(document.querySelectorAll('a[href="archive/index.html"]'), function (link) {
            link.setAttribute('href', homeUrl());
        });

        Array.prototype.forEach.call(document.querySelectorAll('.main-menu a, .mobile-menu a, .main-footer a'), function (link) {
            var href = link.getAttribute('href');
            if (isPlaceholder(href)) {
                // Template parent items open real submenus, including the mobile menu.
                if (link.parentElement && link.parentElement.querySelector('ul')) {
                    link.setAttribute('href', '#');
                    link.addEventListener('click', function (event) { event.preventDefault(); });
                    return;
                }
                disablePlaceholderLink(link);
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', normalizeLegacyTemplateLinks);
    } else {
        normalizeLegacyTemplateLinks();
    }
}(window, document));
