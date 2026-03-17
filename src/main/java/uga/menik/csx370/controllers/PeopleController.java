/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.FollowableUser;
import uga.menik.csx370.services.PeopleService;
import uga.menik.csx370.services.UserService;
import uga.menik.csx370.utility.Utility;

/**
 * Handles /people URL and its sub URL paths.
 */
@Controller
@RequestMapping("/people")
public class PeopleController {

    private final UserService userService;
    private final PeopleService peopleService;

    public PeopleController(UserService userService, PeopleService peopleService) {
        this.userService = userService;
        this.peopleService = peopleService;
    }

    // See LoginController.java to see how to do this.
    // Hint: Add a constructor with @Autowired annotation.

    /**
     * Serves the /people web page.
     * 
     * Note that this accepts a URL parameter called error.
     * The value to this parameter can be shown to the user as an error message.
     * See notes in HashtagSearchController.java regarding URL parameters.
     */
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "error", required = false) String error) {
        // See notes on ModelAndView in BookmarksController.java.
        ModelAndView mv = new ModelAndView("people_page");

        try {
            if (!userService.isAuthenticated()) {
                // If an error occured, you can set the following property with the
                // error message to show the error message to the user.
                // An error message can be optionally specified with a url query parameter too.
                mv.addObject("errorMessage", error);
                mv.addObject("isNoContent", true);
                return mv;
            }
            // Following line populates sample data.
            // You should replace it with actual data from the database.
            // Use the PeopleService instance to find followable users.
            // Use UserService to access logged in userId to exclude.
            var loggedInUser = userService.getLoggedInUser();
            if (loggedInUser == null) {
                mv.addObject("errorMessage", error);
                mv.addObject("isNoContent", true);
                return mv;
            }

            List<FollowableUser> followableUsers = peopleService.getFollowableUsers(loggedInUser.getUserId());
            mv.addObject("users", followableUsers);
            if (followableUsers.isEmpty()) {
                // Enable the following line if you want to show no content message.
                // Do that if your content list is empty.
                mv.addObject("isNoContent", true);
            } else {
                mv.addObject("isNoContent", false);
            }
        } catch (Exception e) {
            mv.addObject("errorMessage", error);
            mv.addObject("isNoContent", true);
        }
        return mv;

    }

    /**
     * This function handles user follow and unfollow.
     * Note the URL has parameters defined as variables ie: {userId} and {isFollow}.
     * Follow and unfollow is handled by submitting a get type form to this URL
     * by specifing the userId and the isFollow variables.
     * Learn more here: https://www.w3schools.com/tags/att_form_method.asp
     * An example URL that is handled by this function looks like below:
     * http://localhost:8081/people/1/follow/false
     * The above URL assigns 1 to userId and false to isFollow.
     */
    @GetMapping("{userId}/follow/{isFollow}")
    public String followUnfollowUser(@PathVariable("userId") String userId,
            @PathVariable("isFollow") Boolean isFollow) {
        System.out.println("User is attempting to follow/unfollow a user:");
        System.out.println("\tuserId: " + userId);
        System.out.println("\tisFollow: " + isFollow);

        try {
            var loggedInUser = userService.getLoggedInUser();
            if (loggedInUser == null) {
                String message = URLEncoder.encode("You must be logged in to follow users.",
                        StandardCharsets.UTF_8);
                return "redirect:/people?error=" + message;
            }
            String loggedInUserId = loggedInUser.getUserId();
            if (isFollow) {
                peopleService.unfollowUser(loggedInUserId, userId);
            } else {
                peopleService.followUser(loggedInUserId, userId);
            }
            return "redirect:/people";
        } catch (Exception e) {
            e.printStackTrace();

            String message = URLEncoder.encode("Failed to (un)follow the user. Please try again.",
                    StandardCharsets.UTF_8);
            return "redirect:/people?error=" + message;
        }

        // Redirect the user with an error message if there was an error.
        String message = URLEncoder.encode("Failed to (un)follow the user. Please try again.",
                StandardCharsets.UTF_8);
        return "redirect:/people?error=" + message;
    }

}
