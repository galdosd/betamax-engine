package com.github.galdosd.betamax.graphics;

import java.util.List;

/** Implementor must be threadsafe for TextureLoadAdvisor methods, will be called from another thread
 *  Interface for advising TextureRegistry on what textures could be loaded next
 */
public interface TextureLoadAdvisor {
    /** Return an ordered list of the most urgently needed textures
     *  All parameters are basically performance advice.
     * @param frameLookahead look ahead this many frames
     */
    List<TextureName> getMostNeededTextures(int frameLookahead);

    /** Return an ordered list of the least urgently needed textures. All parameters are basically performance advice,
     * if it is somehow easier to partially ignore them and return a list of any size, then whatever works is fine.
     *
     * @param frameLookahead Look ahead this many frames
     * @param maxVictims No more than this many need be returned.
     * @param candidates Restrict yourself to these candidates.
     * @return
     */
//    List<TextureName> getLeastNeededTextures(int frameLookahead, int maxVictims, List<TextureName> candidates);
}
