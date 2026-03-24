/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.controllers;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.Post;
import uga.menik.csx370.utility.Utility;

import uga.menik.csx370.services.PostService;

/**
 * Handles /hashtagsearch URL and possibly others.
 * At this point no other URLs.
 */
@Controller
@RequestMapping("/hashtagsearch")
public class HashtagSearchController {

    private final PostService postService;
    public HashtagSearchController(PostService postService) {
        this.postService = postService;
    }
    /**
     * This function handles the /hashtagsearch URL itself.
     * This URL can process a request parameter with name hashtags.
     * In the browser the URL will look something like below:
     * http://localhost:8081/hashtagsearch?hashtags=%23amazing+%23fireworks
     * Note: the value of the hashtags is URL encoded.
     */
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "hashtags") String hashtags) {
        System.out.println("User is searching: " + hashtags);

        // See notes on ModelAndView in BookmarksController.java.
        ModelAndView mv = new ModelAndView("posts_page");

        // Following line populates sample data.
        // You should replace it with actual data from the database.
       List<Post> posts = List.of();
       try {
        java.util.List<String> tags = new java.util.ArrayList<>();
        String[] words = hashtags.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.startsWith("#") && word.length() > 1) {
                String hashtag = word.substring(1).toLowerCase();
                tags.add(hashtag);
            }
        }
        

            if (tags.isEmpty()) {
                mv.addObject("posts", List.of());
                mv.addObject("isNoContent", true);
                return mv;
            }

            posts = postService.searchForHashtag(tags);

        } catch (Exception e) {
            e.printStackTrace();
        }
        mv.addObject("posts", posts);

        if (posts.isEmpty()) {
            mv.addObject("isNoContent", true);
        }

        // If an error occured, you can set the following property with the
        // error message to show the error message to the user.
        // String errorMessage = "Some error occured!";
        // mv.addObject("errorMessage", errorMessage);

        // Enable the following line if you want to show no content message.
        // Do that if your content list is empty.
        // mv.addObject("isNoContent", true);
        
        return mv;
    }
}

