angular.module('arduinoEclipse', ['ui.bootstrap','hc.marked'])
//.config(['marked', function(marked) { marked.setOptions({gfm: true}) }])
.directive('scrollTo', function() {
    return {
      restrict: 'A',
      link: function(scope, element, attrs) {
        element.bind('click', function() {
          scope.$apply(function() {
            var elements = $("[scroll-target='"+ attrs.scrollTo +"']")
            if(elements.length)
              $("body").animate({scrollTop: elements[0].offsetTop - 150}, "slow")
          })
        })
      }
    }
})
.controller('NavBarCtrl', function($scope, $window){
	$scope.navlinks = {
		home: {url: 'index.html'},
		faq: {url: 'faq.html'},
		howto: {url: 'learn.html'},
		bundle_v2: {url: 'bundle-v2.html'},
		plugin_v2: {url: 'plugin-v2.html'},
		plugin_v1: {url: 'plugin-v1.html'},
		linux: {url: '-.html'},
		macos: {url: '-.html'},
		winxp: {url: '-.html'},
		winvista: {url: '-.html'},
		win7: {url: '-.html'},
		win8: {url: '-.html'},
		x64: {url: '-.html'}
	}
	$scope.init = function() {
		for (var item in $scope.navlinks) {
			if ($window.location.href.indexOf($scope.navlinks[item].url) == $window.location.href.length - $scope.navlinks[item].url.length) {
				$scope.navlinks[item].active = true
			}
		}
	}
})