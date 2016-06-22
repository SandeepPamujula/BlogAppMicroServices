//Each of the controllers should be moved to separate files for easy readability 
//In the end they will be assembled as a single js file using requireJS and grunt build system
(function($) {
	//Setup dependencies for the module
	var app = angular.module('mysocial', [ 'ngRoute','textAngular','ngWebsocket' ]);
	app.run(function($http,$rootScope,$location,$log,$websocket) {
		$log.debug("App run...");
          $log.debug(">>---------   Sande 66    ------------------------------------------>   ");
		$rootScope.currentPath = $location.path()
	});
	//ROUTE configurations for all views
	app.config([ '$routeProvider', function($routeProvider) {
		$routeProvider.when('/', {
			templateUrl : 'templates/appHome.html',
			controller : 'AppHomeController'
		}).when('/login', {
			templateUrl : 'templates/login.html',
			controller : 'LoginController'
		}).when('/register', {
			templateUrl : 'templates/register.html',
			controller : 'LoginController'
		}).when('/newPost', {
			templateUrl : 'templates/BlogEdit.html',
			controller : 'BlogController'
		}).otherwise({
			templateUrl : '/404.html'
		});
	} ]).factory('authHttpResponseInterceptor',
			[ '$q', '$location','$log', function($q, $location, $log) {
				return {
					response : function(response) {
						if (response.status === 401) {
							$log.debug("Response 401");
						}
						return response || $q.when(response);
					},
					responseError : function(rejection) {
						if (rejection.status === 401) {
							$log.debug("Response Error 401", rejection);
							$location.path('/login');
						}
						return $q.reject(rejection);
					}
				}
			} ]).config([ '$httpProvider', function($httpProvider) {
		// Http Intercpetor to check auth failures for xhr requests
		$httpProvider.interceptors.push('authHttpResponseInterceptor');
	} ]);
    app.factory('dataService', function() {

          // private object
          var _userObj = {
              userName: "",
              password:"",
              first: "",
              last: "",
              id:""
              
          };

        return {
                getUser: function () {
                    return _userObj;
                },
                setUser: function(user) {
                    _userObj = user;
                }
            };
    });
	//------------------------------------------------------------------------------------------------------------------
	// Controller for the home page with blogs and live users
	//------------------------------------------------------------------------------------------------------------------
	app.controller('AppHomeController', function($http, $log, $scope,
			$rootScope, $websocket, $location,dataService) {
		var controller = this;
		$log.debug("AppHomeController...");
        $rootScope.globals = {
                currentUser:{
                    userName:"",
                    password:""
                }
        };
        
        $rootScope.globals.currentUser = dataService.getUser();
        
         var blogReq = {
                 method: 'GET',
                 url: '/Services/rest/blogs',
                 headers: {
                   'Authorization': "Basic " + btoa($rootScope.globals.currentUser.userName + ":" + $rootScope.globals.currentUser.password)
                 }
            };
        
        
        $http(blogReq)
         .then(function successCallback(data, status, headers, config) {
					$scope.blogs = data.data;
					$scope.loading = false;
				},function errorCallback(data, status, headers, config) {
					$scope.loading = false;
					$scope.error = status;
				});
		var ws=null;
        console.log("login successfull " );
    
        
            var req = {
                 method: 'GET',
                 url: '/Services/rest/user?signedIn=true',
                 headers: {
                   'Authorization': "Basic " + btoa($rootScope.globals.currentUser.userName + ":" + $rootScope.globals.currentUser.password)
                 }
            };
        
        console.log("sande AppHomeController signin: "+$rootScope.globals.currentUser.userName + ":" + $rootScope.globals.currentUser.password);
            
        $http(req)
         .then(function successCallback(data, status, headers, config) {
                    console.log("sande AppHomeController: data "+data.data);
					$scope.connectedUsers = data.data;
					$scope.loading = false;
                
                    var foundUser = _.find(data.data, function(user){ 
                            if (user.userName == $rootScope.globals.currentUser.userName ){
                                $rootScope.globals.currentUser.first = user.first;
                                $rootScope.globals.currentUser.last = user.last;
                                $rootScope.globals.currentUser.id = user.id;
                                dataService.setUser($rootScope.globals.currentUser);
                                return true;
                            }
                    });
            
                    if (foundUser) $log.log("user found");
                    
					//Setup a websocket connection to server using current host
					ws = $websocket.$new('ws://'+$location.host()+':'+$location.port()+'/Services/chat', ['binary', 'base64']); // instance of ngWebsocket, handled by $websocket service
					$log.debug("Web socket established...");
			        ws.$on('$open', function () {
			            $log.debug('Socket is open');
			        });
			        
			        ws.$on('$message', function (data){
			        	 $log.debug('The websocket server has sent the following data:');
			        	 $log.debug(data);
			        	 $log.debug(data.messageType);
			        	 $log.debug(data.event);
			        	 
			        	 if(data.event==="UserLogin"){
			        		 //Add this user to list of users
			        		 var found = false;
			        		 for(var index in $scope.connectedUsers){
			        			 if($scope.connectedUsers[index].id==data.messageObject.id){
			        				 found=true;
			        			 }
			        		 }
			        		 if(!found){
			        			 $log.debug("Adding user to list: "+data.messageObject.first);
			        			 $scope.connectedUsers.push(data.messageObject);
			        			 $scope.$digest();
			        		 }
			        	 }else if(data.event==="chatMessage"){
			        		 //Make sure chat window opensup
			        		 $scope.showChat=true
			        		 $log.debug("Updating chat message: ");
			        		 $log.debug(data.messageObject);
			        		 if($scope.chatMessages===undefined)
			        			 $scope.chatMessages=[];
			        		 
			        		

//var temp = [];
// temp.text = data.data.txt;
//temp.sender = data.data.sender
//$log.debug("text:"+data.data.txt);
//$log.debug( "sender:"+data.data.sender);
//$scope.chatMessages.push(temp);
//$log.debug("Chat Messages: ");
//$log.debug($scope.chatMessages);
//$scope.$digest();
			

			        		 

 var temp = [];
temp.text = data.data.txt;
temp.sender = data.data.sender[0].userName;
 $log.debug("text:"+data.data.txt);
 $log.debug( "sender:"+data.data.sender[0].userName);
 $scope.chatMessages.push(temp);
$log.debug("Chat Messages: ");
 $log.debug($scope.chatMessages);
 $scope.$digest();
			                 
			                 
			        		// $scope.chatMessages.push(data.messageObject);
//			        		 $log.debug("Chat Messages: ");
//			        		 $log.debug($scope.chatMessages);
//			        		 $scope.$digest();
			        	 }
			        });
			        ws.$on('$close', function () {
			            console.log('Web socket closed');
			            ws.$close();
			        });
				},function errorCallback(data, status, headers, config) {
					$scope.loading = false;
					$scope.error = status;
				}); 
        console.log("hii"); 
//            .error(function(data, status, headers, config) {
//					$scope.loading = false;
//					$scope.error = status;
//				});
			$scope.tagSearch = function(){
                
                 var req = {
                     method: 'GET',
                     url: '/Services/rest/blogs?tag='+$scope.searchTag,
                     headers: {
                       'Authorization': "Basic " + btoa($rootScope.globals.currentUser.userName + ":" + $rootScope.globals.currentUser.password)
                     }
                 };
//				$http.get('/Services/rest/blogs?tag='+$scope.searchTag).success(
                $http(req)
                    .then(function successCallback(data, status, headers, config) {
//					function(data, status, headers, config) {
						$scope.blogs = data.data;
						$scope.loading = false;
					},function errorCallback(data, status, headers, config) {
//                         ).error(function(data, status, headers, config) {
						$scope.loading = false;
						$scope.error = status;
					});
			};
			$scope.submitComment = function(comment, blogId){
				$log.debug(comment);
				//var blogId = comment.blogId;
                
                 var req = {
                     method: 'POST',
                     url: '/Services/rest/blogs/'+blogId+'/comments',
                     headers: {
                       'Authorization': "Basic " + btoa($rootScope.globals.currentUser.userName + ":" + $rootScope.globals.currentUser.password + ":"+$rootScope.globals.currentUser.first+ ":" + $rootScope.globals.currentUser.last + ":"+$rootScope.globals.currentUser.id)
                     },
                     data: comment
                 };
                
                $http(req)
                    .then(function successCallback(data, status, headers, config) {
						$scope.loading = false;
						for(var index in $scope.blogs){
							if($scope.blogs[index].id==blogId){
								$log.debug("Pushing the added comment to list");
								$scope.blogs[index].comments.push(comment);
								break;
							}
						}
					},function errorCallback(data, status, headers, config) {
//                         ).error(function(data, status, headers, config) {
						$scope.loading = false;
						$scope.error = status;
					});
			};
		
	//		$scope.sendMessage = function(chatMessage){
//				$log.debug("Sending "+chatMessage);
//				ws.$emit('chatMessage', chatMessage); // send a message to the websocket server
//				$scope.chatMessage="";
				
			    	$scope.sendMessage = function(chatMessage){
					$log.debug("Sending "+chatMessage);
					var msg = { txt :chatMessage,
					sender : $scope.connectedUsers
					//		 txt.user = "demo"
					}
					ws.$emit('chatMessage', msg); // send a message to the websocket server
					$scope.chatMessage="";
				}
	});
	//------------------------------------------------------------------------------------------------------------------
	// Controller for the login view and the registration screen
	//------------------------------------------------------------------------------------------------------------------
	app.controller('LoginController', function($http, $log, $scope, $location,
			$rootScope,dataService) {
        
		var controller = this;
		$scope.isLoadingCompanies = true;
		$http.get('/Services/rest/company').success(
				function(data, status, headers, config) {
					$scope.companies = data;
					$scope.isLoadingCompanies = false;
				}).error(function(data, status, headers, config) {
					$scope.isLoadingCompanies = false;
					$scope.error = status;
				});
		$scope.login = function(user) {
			$log.debug("Logging in user..."+user.userName);
            $rootScope.globals = {
                currentUser:{
                    userName: user.userName,
                    password: user.password
                }
            };
            dataService.setUser($rootScope.globals.currentUser);
            var req = {
                 method: 'POST',
                 url: '/Services/rest/user/auth',
                 headers: {
                   'Authorization': "Basic " + btoa(user.userName + ":" + user.password)
                 }
            };
            
             $http(req)
                .then(function(){
                        console.log("login successfull " +$rootScope.globals.currentUser.userName+ ":" + $rootScope.globals.currentUser.password);
						$rootScope.loggedIn = true;
						$location.path("/");
             });
		};
		$scope.register = function() {
			$log.debug("Navigating to register...");
			$location.path("/register");
		};
		$scope.submitRegister = function(user){
			$log.debug("Registering...");
			$http.post("/Services/rest/user/register", user).success(
					function(data) {
						$log.debug(data);
						$location.path("/");
					});
		}
		$scope.companyChange = function(companyId) {
			$log.debug("Loading sites for company: " + companyId);
			// Load sites
			$http.get('/Services/rest/company/'+companyId+'/sites').success(
					function(data, status, headers, config) {
						$scope.sites = data;
						$scope.isLoadingSites = false;
					}).error(function(data, status, headers, config) {
						$scope.isLoadingSites = false;
						$scope.error = status;
					});
		};
		
		$scope.siteChange = function(companyId, siteId) {
			$log.debug("Loading departments: " + companyId);
			// Load sites
			$http.get('/Services/rest/company/'+companyId+'/sites/'+siteId+'/departments').success(
					function(data, status, headers, config) {
						$scope.departments = data;
						$scope.isLoadingDepts = false;
					}).error(function(data, status, headers, config) {
						$scope.isLoadingDepts = false;
						$scope.error = status;
					});
		};
	});
	//------------------------------------------------------------------------------------------------------------------
	// Controller for the navigation bar.. currently has no functions
	//------------------------------------------------------------------------------------------------------------------
	app.controller('NavbarController',
			function($http, $log, $scope, $rootScope) {
				var controller = this;
				$log.debug("Navbar controller...");

	});

	//------------------------------------------------------------------------------------------------------------------
	// Controller for new blog post view
	//------------------------------------------------------------------------------------------------------------------
	app.controller('BlogController',function($http, $log, $scope, $rootScope, $location,dataService) {
        var controller = this;
        $log.debug("Blog controller...");
        $scope.blog={};
        $scope.blog.content = 'Blog text here...';
        $scope.saveBlog = function(blog){
            
        $rootScope.globals = {
                currentUser:{
                    userName:"",
                    password:""
                }
        };
        
        $rootScope.globals.currentUser = dataService.getUser();

        var req = {
             method: 'POST',
             url: '/Services/rest/blogs',
             headers: {
                       'Authorization': "Basic " + btoa($rootScope.globals.currentUser.userName + ":" + $rootScope.globals.currentUser.password + ":"+$rootScope.globals.currentUser.first+ ":" + $rootScope.globals.currentUser.last + ":"+$rootScope.globals.currentUser.id)
             },
             data: blog
        };

        $http(req)
         .then(function(){
                        $log.debug("Saved blog...");
                        $location.path("/");
                    });
        };
        $scope.cancel = function(blog){
            $location.path("/");
        };
	});

})($);//Passing jquery object just in case 
