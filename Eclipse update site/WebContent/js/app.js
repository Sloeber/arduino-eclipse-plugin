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
.controller('NavBarCtrl', function($scope, $window) {
	$scope.navlinks = {
		"home": {url: 'index.html'},
		"faq": {url: 'faq.shtml'},
		"howto": {url: 'how_to.shtml'},
    "starting": {url: 'installAdvice.shtml'},
    "components": {url: 'getting-started.shtml'},
		"linux": {url: 'stable-linux.html'},
    "osx": {url: 'stable-osx.html'},
    "win": {url: 'stable-win.html'},
    "linux_nightly": {url: 'nightly-linux.html'},
    "osx_nightly": {url: 'nightly-osx.html'},
    "win_nightly": {url: 'nightly-win.html'},
    "archived_1_x": {url: 'archived-1.x.html'},
    "archived_2_2": {url: 'archived-2.2.html'}
	}
	$scope.init = function() {
		for (var item in $scope.navlinks) {
			if ($window.location.href.indexOf('/' + $scope.navlinks[item].url) > 0) {
				$scope.navlinks[item].active = true
			}
		}
	}
})
.controller('AffixCtrl', function($scope, $location) {
  $scope.select = function(tab) {
    for (prop in $scope.tab) { if ($scope.tab.hasOwnProperty(prop)) { delete $scope.tab[prop] } };
    $scope.tab[tab] = {active: true};
  }
  $scope.tab = {'welcome': true};
  $scope.init = function() {
    if($location.path()) {
      $scope.select($location.path().substring(1))
    }
  }
})
.controller('PillsCtrl', function($scope, $location) {
  $scope.init = function() {
    if($location.path()) {
      $('a[href="#' + $location.path().substring(1) + '"]').tab('show')
    }
  }
})