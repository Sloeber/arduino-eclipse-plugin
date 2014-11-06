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
		"home": {url: 'index.html'},
		"faq": {url: 'faq.html'},
		"howto": {url: 'learn.html'},
		"linux": {url: '-.html'},
    "osx": {url: '-.html'},
    "win": {url: '-.html'},
    "linux_nightly": {url: 'nightly-linux.html'},
    "osx_nightly": {url: 'nightly-osx.html'},
    "win_nightly": {url: 'nightly-win.html'},
		"archivied-v1": {url: '-.html'}
	}
	$scope.init = function() {
		for (var item in $scope.navlinks) {
			if ($window.location.href.indexOf('/' + $scope.navlinks[item].url) > 0) {
				$scope.navlinks[item].active = true
			}
		}
	}
})